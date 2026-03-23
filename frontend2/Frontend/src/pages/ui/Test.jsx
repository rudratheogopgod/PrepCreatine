import { useState, useEffect } from "react";
import toast, { Toaster } from "react-hot-toast";
import api from "../../api/axios";

export default function Test() {
  const [mode, setMode] = useState("full");
  const [subject, setSubject] = useState("");
  const [chapter, setChapter] = useState("");
  const [duration, setDuration] = useState(30);

  const [started, setStarted] = useState(false);
  const [testSession, setTestSession] = useState(null); // { id, questions: [...] }
  const [currentQ, setCurrentQ] = useState(0);
  const [answers, setAnswers] = useState({});
  const [timeLeft, setTimeLeft] = useState(0);
  const [submitted, setSubmitted] = useState(false);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const examId = "JEE"; // default exam — can be wired to UserContext later

  // Syllabus for display (static) — real questions come from backend
  const syllabus = {
    Physics: ["Mechanics", "Thermodynamics", "Electrostatics", "Optics"],
    Chemistry: ["Mole Concept", "Organic Chemistry", "Electrochemistry"],
    Mathematics: ["Calculus", "Algebra", "Coordinate Geometry", "Probability"],
  };

  // Timer
  useEffect(() => {
    if (!started || submitted || timeLeft <= 0) return;
    const timer = setInterval(() => {
      setTimeLeft((t) => {
        if (t <= 1) { clearInterval(timer); handleSubmit(); return 0; }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [started, submitted]);

  const handleStart = async () => {
    setLoading(true);
    try {
      const params = {
        examId,
        questions: 10,
        timeLimitMins: duration,
      };
      if (mode === "custom" && subject) params.subjectId = subject;
      if (mode === "custom" && chapter) params.topicId = chapter;

      const res = await api.post("/tests/start", null, { params });
      setTestSession(res.data);
      setTimeLeft(duration * 60);
      setStarted(true);
      setCurrentQ(0);
      setAnswers({});
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to start test. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (opt) => {
    setAnswers({ ...answers, [currentQ]: opt });
  };

  const handleNext = () => {
    const questions = testSession?.questions ?? [];
    if (currentQ < questions.length - 1) {
      setCurrentQ(currentQ + 1);
    } else {
      handleSubmit();
    }
  };

  const handleSubmit = async () => {
    if (submitted) return;
    setSubmitted(true);
    const questions = testSession?.questions ?? [];
    const answersPayload = questions.map((q, i) => ({
      questionId: q.id,
      selectedOption: answers[i] ?? null,
    }));
    try {
      const res = await api.post(`/tests/${testSession.id}/submit`, {
        answers: answersPayload,
      });
      setResult(res.data);
    } catch {
      // fallback: compute locally
      let correct = 0;
      questions.forEach((q, i) => {
        if (answers[i] === q.correctAnswer) correct++;
      });
      setResult({ score: correct, totalQuestions: questions.length });
    }
  };

  const questions = testSession?.questions ?? [];
  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;

  // ── CONFIG SCREEN ──────────────────────────────────────────────────────────
  if (!started) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-blue-50 p-6">
        <Toaster position="top-right" />
        <div className="max-w-5xl mx-auto">
          <h1 className="text-4xl font-bold mb-2 text-gray-900">AI Smart Test Engine ⚡</h1>
          <p className="text-gray-500 mb-8">Adaptive test based on your learning pattern</p>

          {/* MODE */}
          <div className="mb-8 flex gap-4">
            {["full", "custom"].map((m) => (
              <button
                key={m}
                onClick={() => setMode(m)}
                className={`px-6 py-3 rounded-2xl font-medium transition ${
                  mode === m
                    ? "bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-lg"
                    : "bg-gray-100 hover:bg-gray-200"
                }`}
              >
                {m === "full" ? "Full Syllabus" : "Custom Test"}
              </button>
            ))}
          </div>

          {/* SUBJECT SELECTOR */}
          {mode === "custom" && (
            <div className="mb-8 p-6 bg-white rounded-3xl shadow border space-y-4">
              <h3 className="font-semibold">Select Focus Area</h3>
              <select
                className="w-full p-3 rounded-xl border"
                onChange={(e) => { setSubject(e.target.value); setChapter(""); }}
              >
                <option value="">Select Subject</option>
                {Object.keys(syllabus).map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
              {subject && (
                <select
                  className="w-full p-3 rounded-xl border"
                  onChange={(e) => setChapter(e.target.value)}
                >
                  <option value="">Select Chapter (optional)</option>
                  {syllabus[subject].map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              )}
            </div>
          )}

          {/* DURATION */}
          <div className="mb-10 p-6 bg-white rounded-3xl shadow border">
            <h3 className="font-semibold mb-4">Duration</h3>
            <input
              type="range" min={5} max={180} value={duration}
              onChange={(e) => setDuration(Number(e.target.value))}
              className="w-full accent-purple-600"
            />
            <p className="text-sm text-gray-500 mt-2">{duration} minutes</p>
          </div>

          <button
            onClick={handleStart}
            disabled={loading}
            className="px-10 py-4 bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-2xl shadow-lg hover:scale-105 transition disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {loading ? "Generating Questions..." : "🚀 Start Test"}
          </button>
        </div>
      </div>
    );
  }

  // ── TEST SCREEN ────────────────────────────────────────────────────────────
  if (!submitted) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-blue-50 p-6">
        <div className="max-w-4xl mx-auto">
          <div className="flex justify-between items-center mb-6">
            <h2 className="font-semibold text-gray-700">
              Question {currentQ + 1}/{questions.length}
            </h2>
            <div className="px-4 py-2 bg-purple-100 text-purple-700 rounded-xl font-mono">
              ⏳ {String(minutes).padStart(2, "0")}:{String(seconds).padStart(2, "0")}
            </div>
          </div>

          <div className="w-full h-2 bg-gray-200 rounded-full mb-6">
            <div
              className="h-2 bg-gradient-to-r from-purple-500 to-indigo-600 rounded-full transition-all"
              style={{ width: `${((currentQ + 1) / questions.length) * 100}%` }}
            />
          </div>

          <div className="bg-white p-8 rounded-3xl shadow-lg">
            <p className="text-lg mb-6 text-gray-800">
              {questions[currentQ]?.questionText ?? questions[currentQ]?.question}
            </p>
            <div className="grid gap-4">
              {(questions[currentQ]?.options ?? []).map((opt) => (
                <div
                  key={opt}
                  onClick={() => handleSelect(opt)}
                  className={`p-4 rounded-xl border cursor-pointer transition ${
                    answers[currentQ] === opt
                      ? "bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow"
                      : "hover:bg-gray-100"
                  }`}
                >
                  {opt}
                </div>
              ))}
            </div>
          </div>

          <div className="flex gap-4 mt-6">
            <button
              onClick={handleNext}
              className="px-8 py-3 bg-blue-500 text-white rounded-2xl shadow hover:scale-105 transition"
            >
              {currentQ < questions.length - 1 ? "Next →" : "Submit Test"}
            </button>
            <button
              onClick={handleSubmit}
              className="px-8 py-3 bg-gray-200 text-gray-700 rounded-2xl hover:bg-gray-300 transition"
            >
              End Test
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ── RESULT SCREEN ──────────────────────────────────────────────────────────
  const score = result?.score ?? 0;
  const total = result?.totalQuestions ?? questions.length;
  const accuracy = total > 0 ? Math.round((score / total) * 100) : 0;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-blue-50 p-6">
      <div className="max-w-4xl mx-auto">
        <h2 className="text-3xl font-bold mb-6">Your Performance 🚀</h2>

        <div className="p-6 bg-white rounded-3xl shadow mb-6">
          <h3 className="text-xl font-semibold">Score: {score}/{total}</h3>
          <p className="text-gray-500">Accuracy: {accuracy}%</p>
        </div>

        <div className="mb-6">
          <div className="w-full h-4 bg-gray-200 rounded-full">
            <div
              className="h-4 bg-gradient-to-r from-green-400 to-green-600 rounded-full"
              style={{ width: `${accuracy}%` }}
            />
          </div>
        </div>

        <p className="text-lg font-semibold text-gray-800">
          {accuracy === 100
            ? "🔥 Perfect! You're exam ready!"
            : accuracy >= 50
            ? "💪 Good job! Keep focusing on weak areas."
            : "🚀 Keep practicing — consistency is key!"}
        </p>

        {result?.aiAnalysis && (
          <div className="mt-6 p-4 bg-purple-50 rounded-2xl border border-purple-100">
            <p className="text-sm font-semibold text-purple-700 mb-1">🤖 AI Analysis</p>
            <p className="text-sm text-gray-700">{result.aiAnalysis}</p>
          </div>
        )}

        <button
          onClick={() => {
            setStarted(false);
            setSubmitted(false);
            setTestSession(null);
            setResult(null);
            setAnswers({});
          }}
          className="mt-8 px-8 py-3 bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-2xl shadow hover:scale-105 transition"
        >
          Take Another Test
        </button>
      </div>
    </div>
  );
}
