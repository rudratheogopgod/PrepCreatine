import React, { useState } from "react";

type Mode = "full" | "custom";

type SyllabusType = {
  [subject: string]: {
    [chapter: string]: string[];
  };
};

type Props = {
  onStart: (config: {
    mode: Mode;
    subject?: string;
    chapter?: string;
    topic?: string;
    duration: number;
  }) => void;
};

export default function TestConfig({ onStart }: Props) {
  const [mode, setMode] = useState<Mode>("full");
  const [subject, setSubject] = useState<string>("");
  const [chapter, setChapter] = useState<string>("");
  const [topic, setTopic] = useState<string>("");
  const [duration, setDuration] = useState<number>(60);

  const syllabus: SyllabusType = {
    Physics: {
      Mechanics: ["Motion", "Force"],
      Thermodynamics: ["Heat", "Laws"],
    },
    Math: {
      Algebra: ["Quadratic", "Matrices"],
      Calculus: ["Derivative", "Integration"],
    },
  };

  const handleStart = () => {
    onStart({
      mode,
      subject,
      chapter,
      topic,
      duration,
    });

    // 🔥 BACKEND
    // TODO: POST /api/test/config
  };

  return (
    <div className="max-w-5xl mx-auto">

      <h1 className="text-4xl font-bold mb-2 text-gray-900">
        AI Smart Test Engine ⚡
      </h1>

      <p className="text-gray-500 mb-8">
        Adaptive test based on your learning pattern
      </p>

      {/* MODE */}
      <div className="mb-8 flex gap-4">
        <button
          onClick={() => setMode("full")}
          className={`px-6 py-3 rounded-2xl ${mode === "full"
              ? "bg-gradient-to-r from-purple-600 to-indigo-600 text-white"
              : "bg-gray-100"
            }`}
        >
          Full Syllabus
        </button>

        <button
          onClick={() => setMode("custom")}
          className={`px-6 py-3 rounded-2xl ${mode === "custom"
              ? "bg-gradient-to-r from-purple-600 to-indigo-600 text-white"
              : "bg-gray-100"
            }`}
        >
          Custom Test
        </button>
      </div>

      {/* FULL MODE */}
      {mode === "full" && (
        <div className="mb-8 p-6 bg-white rounded-3xl shadow border">

          <h3 className="font-semibold mb-4">Select Chapter</h3>

          {Object.keys(syllabus).map((sub) => (
            <div key={sub} className="mb-4">

              <p className="font-semibold text-gray-700">{sub}</p>

              <div className="flex flex-wrap gap-2 mt-2">
                {Object.keys(syllabus[sub]).map((chap) => (
                  <button
                    key={chap}
                    onClick={() => setChapter(chap)}
                    className={`px-3 py-1 rounded-xl border ${chapter === chap
                        ? "bg-purple-600 text-white"
                        : "hover:bg-gray-100"
                      }`}
                  >
                    {chap}
                  </button>
                ))}
              </div>

            </div>
          ))}

        </div>
      )}

      {/* CUSTOM MODE */}
      {mode === "custom" && (
        <div className="mb-8 p-6 bg-white rounded-3xl shadow border space-y-4">

          <select
            className="w-full p-3 rounded-xl border"
            onChange={(e) => setSubject(e.target.value)}
          >
            <option>Select Subject</option>
            {Object.keys(syllabus).map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>

          {subject && (
            <select
              className="w-full p-3 rounded-xl border"
              onChange={(e) => setChapter(e.target.value)}
            >
              <option>Select Chapter</option>
              {Object.keys(syllabus[subject]).map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          )}

          {chapter && (
            <select
              className="w-full p-3 rounded-xl border"
              onChange={(e) => setTopic(e.target.value)}
            >
              <option>Select Topic</option>
              {syllabus[subject][chapter].map((t) => (
                <option key={t}>{t}</option>
              ))}
            </select>
          )}

        </div>
      )}

      {/* DURATION */}
      <div className="mb-10 p-6 bg-white rounded-3xl shadow border">

        <h3 className="font-semibold mb-4">Duration</h3>

        <input
          type="range"
          min={30}
          max={180}
          value={duration}
          onChange={(e) => setDuration(Number(e.target.value))}
          className="w-full accent-purple-600"
        />

        <p className="text-sm text-gray-500 mt-2">
          {duration} seconds
        </p>

      </div>

      <button
        onClick={handleStart}
        className="px-10 py-4 bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-2xl shadow-lg hover:scale-105 transition"
      >
        🚀 Start Test
      </button>

    </div>
  );
}