#include <jni.h>
#include <essentia/algorithmfactory.h>
#include <essentia/essentia.h>
#include <algorithm>
#include <cmath>
#include <memory>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

using essentia::Real;
using essentia::standard::Algorithm;
using essentia::standard::AlgorithmFactory;

namespace {
std::once_flag initFlag;
const char* NAMES[12] = {"C","C#","D","Eb","E","F","F#","G","Ab","A","Bb","B"};

std::string escapeJson(const std::string& s) {
    std::string out;
    for (char c : s) {
        if (c == '"' || c == '\\') out += '\\';
        out += c;
    }
    return out;
}

struct ChordFrame { long startMs; long endMs; std::string chord; float confidence; };

std::pair<std::string,float> matchChord(const std::vector<Real>& hpcp) {
    float best = -1.f, second = -1.f;
    int bestRoot = 0; bool bestMinor = false;
    for (int root = 0; root < 12; ++root) {
        for (int minor = 0; minor < 2; ++minor) {
            int third = (root + (minor ? 3 : 4)) % 12;
            int fifth = (root + 7) % 12;
            float wanted = hpcp[root] + 0.9f*hpcp[third] + 0.8f*hpcp[fifth];
            float other = 0.f;
            for (int i=0;i<12;++i) if (i!=root && i!=third && i!=fifth) other += hpcp[i];
            float score = wanted - 0.18f*other;
            if (score > best) { second=best; best=score; bestRoot=root; bestMinor=minor; }
            else if (score > second) second=score;
        }
    }
    float conf = std::clamp((best-second) / (std::abs(best)+0.15f), 0.f, 1.f);
    return {std::string(NAMES[bestRoot]) + (bestMinor ? "m" : ""), conf};
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_edward_ukuleleai_data_analysis_EssentiaNative_analyze(JNIEnv* env, jobject, jfloatArray input, jint sampleRate) {
    try {
        std::call_once(initFlag, [](){ essentia::init(); });
        const jsize n = env->GetArrayLength(input);
        std::vector<Real> samples((size_t)n);
        env->GetFloatArrayRegion(input, 0, n, samples.data());
        if (samples.empty() || sampleRate <= 0) throw std::runtime_error("Empty audio");

        AlgorithmFactory& f = AlgorithmFactory::instance();
        std::unique_ptr<Algorithm> rhythm(f.create("RhythmExtractor2013", "method", "degara"));
        Real bpm=0, rhythmConfidence=0; std::vector<Real> ticks, estimates, bpmIntervals;
        rhythm->input("signal").set(samples);
        rhythm->output("bpm").set(bpm); rhythm->output("ticks").set(ticks);
        rhythm->output("confidence").set(rhythmConfidence); rhythm->output("estimates").set(estimates);
        rhythm->output("bpmIntervals").set(bpmIntervals); rhythm->compute();
        if (!std::isfinite(bpm) || bpm < 40 || bpm > 220) bpm = 80;

        const int frameSize = 4096;
        const int hop = 2048;
        std::unique_ptr<Algorithm> window(f.create("Windowing", "type", "hann", "size", frameSize));
        std::unique_ptr<Algorithm> spectrumAlg(f.create("Spectrum", "size", frameSize));
        std::unique_ptr<Algorithm> peaks(f.create("SpectralPeaks", "sampleRate", (Real)sampleRate,
            "minFrequency", 40.0, "maxFrequency", std::min(5000.0, sampleRate/2.0), "maxPeaks", 100,
            "orderBy", "magnitude", "magnitudeThreshold", 1e-6));
        std::unique_ptr<Algorithm> hpcpAlg(f.create("HPCP", "size", 12, "sampleRate", (Real)sampleRate,
            "minFrequency", 40.0, "maxFrequency", std::min(5000.0, sampleRate/2.0),
            "normalized", "unitMax", "referenceFrequency", 440.0));

        std::vector<Real> frame(frameSize,0), win, spectrum, freq, mag, hpcp;
        std::vector<Real> global(12,0);
        std::vector<ChordFrame> raw;
        for (int start=0; start<n; start+=hop) {
            std::fill(frame.begin(),frame.end(),0);
            int count=std::min(frameSize,(int)n-start);
            std::copy(samples.begin()+start,samples.begin()+start+count,frame.begin());
            window->input("frame").set(frame); window->output("frame").set(win); window->compute();
            spectrumAlg->input("frame").set(win); spectrumAlg->output("spectrum").set(spectrum); spectrumAlg->compute();
            peaks->input("spectrum").set(spectrum); peaks->output("frequencies").set(freq); peaks->output("magnitudes").set(mag); peaks->compute();
            hpcpAlg->input("frequencies").set(freq); hpcpAlg->input("magnitudes").set(mag); hpcpAlg->output("hpcp").set(hpcp); hpcpAlg->compute();
            if (hpcp.size()!=12) continue;
            for(int i=0;i<12;++i) global[i]+=hpcp[i];
            auto matched=matchChord(hpcp);
            long startMs=(long)std::llround(start*1000.0/sampleRate);
            long endMs=(long)std::llround(std::min(start+hop,(int)n)*1000.0/sampleRate);
            raw.push_back({startMs,endMs,matched.first,matched.second});
        }

        std::unique_ptr<Algorithm> keyAlg(f.create("Key", "profileType", "edma", "usePolyphony", true, "useThreeChords", true));
        std::string key="C", scale="major"; Real keyStrength=0, firstToSecond=0;
        keyAlg->input("pcp").set(global); keyAlg->output("key").set(key); keyAlg->output("scale").set(scale);
        keyAlg->output("strength").set(keyStrength); keyAlg->output("firstToSecondRelativeStrength").set(firstToSecond); keyAlg->compute();

        std::vector<ChordFrame> merged;
        for (size_t i=0;i<raw.size();++i) {
            std::string chosen=raw[i].chord;
            if (i>0 && i+1<raw.size() && raw[i-1].chord==raw[i+1].chord) chosen=raw[i-1].chord;
            if (merged.empty() || merged.back().chord!=chosen) merged.push_back({raw[i].startMs,raw[i].endMs,chosen,raw[i].confidence});
            else { merged.back().endMs=raw[i].endMs; merged.back().confidence=(merged.back().confidence+raw[i].confidence)*0.5f; }
        }
        for (size_t i=1;i<merged.size();) {
            if (merged[i].endMs-merged[i].startMs < 450) {
                merged[i-1].endMs=merged[i].endMs; i=merged.erase(merged.begin()+i)-merged.begin();
            } else ++i;
        }

        long durationMs=(long)std::llround(n*1000.0/sampleRate);
        std::ostringstream out; out.setf(std::ios::fixed); out.precision(4);
        out << "{\"schemaVersion\":1,\"source\":\"essentia-offline\",\"durationMs\":"<<durationMs
            <<",\"bpm\":"<<bpm<<",\"key\":\""<<escapeJson(key)<<"\",\"scale\":\""<<escapeJson(scale)
            <<"\",\"keyConfidence\":"<<keyStrength<<",\"chords\":[";
        for(size_t i=0;i<merged.size();++i){ if(i) out<<','; auto& c=merged[i];
            out<<"{\"startMs\":"<<c.startMs<<",\"endMs\":"<<c.endMs<<",\"chord\":\""<<c.chord<<"\",\"confidence\":"<<c.confidence<<"}";
        }
        out << "]}";
        return env->NewStringUTF(out.str().c_str());
    } catch (const std::exception& e) {
        std::string msg=std::string("Essentia analysis failed: ")+e.what();
        jclass ex=env->FindClass("java/lang/IllegalStateException"); env->ThrowNew(ex,msg.c_str()); return nullptr;
    }
}
