import { useState, useEffect } from "react";
import { CheckCircle, Clock, PlayCircle } from "lucide-react";
import toast, { Toaster } from "react-hot-toast";
import api from "../../api/axios";

export default function Planner() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [completing, setCompleting] = useState(null); // topicId being completed

  const fetchPlan = async () => {
    try {
      const res = await api.get("/planner/today");
      // backend returns { sessions: [...], generatedAt: "..." }
      setSessions(res.data?.sessions ?? []);
    } catch (err) {
      toast.error("Could not load today's plan. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlan();
  }, []);

  const handleComplete = async (session) => {
    if (session.completed) return;
    setCompleting(session.topicId ?? session.id);
    try {
      await api.post("/planner/today/session-complete", {
        topicId: session.topicId ?? session.id,
        sessionType: session.type ?? "study",
        actualMins: session.durationMins ?? 45,
      });
      toast.success(`✅ "${session.topic ?? session.subject}" marked complete!`);
      // Refresh plan to reflect updated statuses
      await fetchPlan();
    } catch (err) {
      toast.error("Failed to mark session complete.");
    } finally {
      setCompleting(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-400">
        Generating your study plan...
      </div>
    );
  }

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <Toaster position="top-right" />
      <h1 className="text-2xl font-bold mb-2 text-gray-900">Today&apos;s Study Plan 📚</h1>
      <p className="text-sm text-gray-500 mb-6">
        {sessions.length} session{sessions.length !== 1 ? "s" : ""} scheduled for today
      </p>

      {sessions.length === 0 ? (
        <div className="p-8 bg-white rounded-2xl shadow text-center text-gray-500">
          No sessions planned for today. Check back tomorrow! 🎉
        </div>
      ) : (
        <div className="space-y-4">
          {sessions.map((item, index) => {
            const isCompleted = item.completed;
            const isLoading = completing === (item.topicId ?? item.id);
            const label = item.topic ?? item.subject ?? "Study Session";
            const type = item.type ?? item.sessionType ?? "Study";
            const time = item.scheduledTime ?? item.time ?? "";

            return (
              <div
                key={index}
                className={`p-4 border rounded-xl flex items-center justify-between transition ${
                  isCompleted
                    ? "bg-green-50 border-green-200 opacity-70"
                    : "bg-white border-gray-200 hover:shadow-md"
                }`}
              >
                <div className="flex gap-4 items-center">
                  <div className="text-gray-400">
                    {isCompleted ? (
                      <CheckCircle size={20} className="text-green-500" />
                    ) : (
                      <Clock size={20} />
                    )}
                  </div>
                  <div>
                    {time && (
                      <p className="text-xs text-gray-400 font-mono mb-0.5">{time}</p>
                    )}
                    <p className="font-semibold text-gray-800">{label}</p>
                    <div className="flex gap-2 mt-1 flex-wrap">
                      <span className="text-xs uppercase bg-gray-100 px-2 py-0.5 rounded text-gray-600">
                        {type}
                      </span>
                      {item.durationMins && (
                        <span className="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded">
                          {item.durationMins} min
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => handleComplete(item)}
                  disabled={isCompleted || isLoading}
                  className={`px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-1.5 transition ${
                    isCompleted
                      ? "bg-green-100 text-green-700 cursor-default"
                      : "bg-blue-600 text-white hover:bg-blue-700 active:scale-95"
                  } disabled:opacity-50 disabled:cursor-not-allowed`}
                >
                  {isCompleted ? (
                    <>
                      <CheckCircle size={14} /> Completed
                    </>
                  ) : isLoading ? (
                    "Saving..."
                  ) : (
                    <>
                      <PlayCircle size={14} /> Start Session
                    </>
                  )}
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}