import { Brain, Flame, TrendingUp, BarChart3 } from "lucide-react";

export default function Progress() {

  // 🔥 DUMMY DATA (backend se replace hoga)
  const totalTests = 12;
  const avgScore = 72;
  const accuracy = 68;
  const streak = 5;

  const weakTopics = ["Thermodynamics", "Algebra"];
  const strongTopics = ["Mechanics", "Integration"];

  const recentScores = [60, 65, 70, 75, 80, 72];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-blue-50 p-6">

      <div className="max-w-6xl mx-auto">

        {/* HEADER */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">
            Your Progress 📊
          </h1>
          <p className="text-gray-500">
            AI-powered insights into your learning journey
          </p>
        </div>

        {/* 🔥 TOP CARDS */}
        <div className="grid md:grid-cols-4 gap-6 mb-8">

          <div className="p-6 bg-white rounded-3xl shadow">
            <p className="text-sm text-gray-500">Tests Given</p>
            <h2 className="text-2xl font-bold">{totalTests}</h2>
          </div>

          <div className="p-6 bg-white rounded-3xl shadow">
            <p className="text-sm text-gray-500">Average Score</p>
            <h2 className="text-2xl font-bold">{avgScore}%</h2>
          </div>

          <div className="p-6 bg-white rounded-3xl shadow">
            <p className="text-sm text-gray-500">Accuracy</p>
            <h2 className="text-2xl font-bold">{accuracy}%</h2>
          </div>

          <div className="p-6 bg-white rounded-3xl shadow flex items-center gap-2">
            <Flame className="text-orange-500" />
            <div>
              <p className="text-sm text-gray-500">Streak</p>
              <h2 className="text-2xl font-bold">{streak} days</h2>
            </div>
          </div>

        </div>

        {/* 🔥 PERFORMANCE GRAPH */}
        <div className="bg-white p-6 rounded-3xl shadow mb-8">

          <h2 className="font-semibold mb-4 flex items-center gap-2">
            <TrendingUp size={18} /> Performance Trend
          </h2>

          <div className="flex items-end gap-3 h-40">
            {recentScores.map((score, i) => (
              <div
                key={i}
                className="flex-1 bg-gradient-to-t from-blue-500 to-indigo-600 rounded-lg"
                style={{ height: `${score}%` }}
              />
            ))}
          </div>

        </div>

        {/* 🔥 WEAK + STRONG */}
        <div className="grid md:grid-cols-2 gap-6">

          {/* WEAK */}
          <div className="bg-white p-6 rounded-3xl shadow">

            <h2 className="font-semibold mb-4 flex items-center gap-2 text-red-500">
              <Brain size={18} /> Weak Areas
            </h2>

            <div className="flex gap-2 flex-wrap">
              {weakTopics.map((t) => (
                <span
                  key={t}
                  className="px-3 py-1 bg-red-100 text-red-600 rounded-full text-xs"
                >
                  {t}
                </span>
              ))}
            </div>

            {/* BACKEND */}
            {/* TODO: GET /api/analysis */}
          </div>

          {/* STRONG */}
          <div className="bg-white p-6 rounded-3xl shadow">

            <h2 className="font-semibold mb-4 flex items-center gap-2 text-green-600">
              <BarChart3 size={18} /> Strong Areas
            </h2>

            <div className="flex gap-2 flex-wrap">
              {strongTopics.map((t) => (
                <span
                  key={t}
                  className="px-3 py-1 bg-green-100 text-green-600 rounded-full text-xs"
                >
                  {t}
                </span>
              ))}
            </div>

          </div>

        </div>

        {/* 🔥 AI INSIGHT */}
        <div className="mt-8 p-6 rounded-3xl bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-lg">

          <h2 className="font-semibold mb-2">
            🤖 AI Insight
          </h2>

          <p className="text-sm">
            You're improving steadily! Focus more on weak areas to boost your score by 15% 🚀
          </p>

        </div>

      </div>
    </div>
  );
}