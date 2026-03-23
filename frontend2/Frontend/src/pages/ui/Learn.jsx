import { useState, useEffect, useRef } from "react";
import { Send, Plus, Brain, Trash2 } from "lucide-react";
import toast, { Toaster } from "react-hot-toast";
import api from "../../api/axios";

export default function Learn() {
  const [conversations, setConversations] = useState([]);
  const [currentId, setCurrentId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [streaming, setStreaming] = useState(false);
  const bottomRef = useRef(null);

  // Load conversations on mount
  useEffect(() => {
    api.get("/conversations")
      .then((res) => {
        setConversations(res.data || []);
        if (res.data?.length > 0) {
          loadMessages(res.data[0].id);
          setCurrentId(res.data[0].id);
        }
      })
      .catch(() => {
        // Show welcome message even with no API
        setMessages([{ role: "ai", text: "Hi! I'm your AI tutor powered by Gemini. Ask me anything 🚀" }]);
      });
  }, []);

  // Scroll to bottom on new messages
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const loadMessages = async (convId) => {
    try {
      const res = await api.get(`/conversations/${convId}/messages`);
      const mapped = (res.data || []).map((m) => ({
        role: m.role === "USER" ? "user" : "ai",
        text: m.content,
      }));
      setMessages(
        mapped.length > 0
          ? mapped
          : [{ role: "ai", text: "Hi! I'm your AI tutor. Ask me anything 🚀" }]
      );
    } catch {
      setMessages([{ role: "ai", text: "Hi! I'm your AI tutor. Ask me anything 🚀" }]);
    }
  };

  const handleNewChat = async () => {
    // A new chat is created on first message; just reset the UI
    setCurrentId(null);
    setMessages([{ role: "ai", text: "New conversation started 🚀 Ask anything!" }]);
  };

  const handleDeleteConversation = async (convId, e) => {
    e.stopPropagation();
    try {
      await api.delete(`/conversations/${convId}`);
      setConversations((prev) => prev.filter((c) => c.id !== convId));
      if (currentId === convId) {
        setCurrentId(null);
        setMessages([{ role: "ai", text: "Hi! I'm your AI tutor. Ask me anything 🚀" }]);
      }
      toast.success("Conversation deleted.");
    } catch {
      toast.error("Failed to delete conversation.");
    }
  };

  const handleSend = async () => {
    if (!input.trim() || streaming) return;

    const userMsg = { role: "user", text: input };
    setMessages((prev) => [...prev, userMsg]);
    const question = input;
    setInput("");
    setStreaming(true);

    // Add a placeholder AI message to stream tokens into
    let aiText = "";
    setMessages((prev) => [...prev, { role: "ai", text: "▋" }]);

    try {
      const response = await fetch(
        `${import.meta.env.VITE_API_URL || "http://localhost:8080/api"}/chat`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
          },
          body: JSON.stringify({
            message: question,
            conversationId: currentId ?? undefined,
          }),
        }
      );

      if (!response.ok) throw new Error("Chat request failed");

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value);
        const lines = chunk.split("\n");
        for (const line of lines) {
          if (line.startsWith("data:")) {
            const data = line.slice(5).trim();
            if (data === "[DONE]") continue;
            try {
              const parsed = JSON.parse(data);
              if (parsed.token) {
                aiText += parsed.token;
                setMessages((prev) => {
                  const updated = [...prev];
                  updated[updated.length - 1] = { role: "ai", text: aiText + "▋" };
                  return updated;
                });
              }
              if (parsed.conversationId && !currentId) {
                setCurrentId(parsed.conversationId);
                // Refresh conversation list
                api.get("/conversations").then((r) => setConversations(r.data || []));
              }
            } catch {/* ignore parse errors */}
          }
        }
      }

      // Remove cursor
      setMessages((prev) => {
        const updated = [...prev];
        updated[updated.length - 1] = { role: "ai", text: aiText };
        return updated;
      });
    } catch {
      setMessages((prev) => {
        const updated = [...prev];
        updated[updated.length - 1] = { role: "ai", text: "Sorry, I couldn't respond right now. Please try again." };
        return updated;
      });
      toast.error("Failed to get AI response.");
    } finally {
      setStreaming(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="flex h-[calc(100vh-64px)] bg-slate-50">
      <Toaster position="top-right" />

      {/* SIDEBAR */}
      <div className="w-64 bg-white border-r p-4 flex flex-col">
        <button
          onClick={handleNewChat}
          className="flex items-center gap-2 bg-blue-500 text-white px-4 py-2 rounded-xl mb-4 hover:bg-blue-600 transition"
        >
          <Plus size={16} /> New Conversation
        </button>

        <p className="text-xs text-gray-400 mb-3 uppercase tracking-wider">History</p>

        <div className="space-y-1.5 text-sm overflow-y-auto flex-1">
          {conversations.map((chat) => (
            <div
              key={chat.id}
              onClick={() => { setCurrentId(chat.id); loadMessages(chat.id); }}
              className={`p-2 rounded-lg cursor-pointer flex items-center justify-between group ${
                currentId === chat.id
                  ? "bg-blue-100 text-blue-600"
                  : "hover:bg-gray-100 text-gray-700"
              }`}
            >
              <span className="truncate">{chat.title || "Conversation"}</span>
              <button
                onClick={(e) => handleDeleteConversation(chat.id, e)}
                className="hidden group-hover:flex text-gray-400 hover:text-red-500 ml-1"
              >
                <Trash2 size={14} />
              </button>
            </div>
          ))}
          {conversations.length === 0 && (
            <p className="text-xs text-gray-400 text-center mt-4">No conversations yet.</p>
          )}
        </div>
      </div>

      {/* MAIN CHAT */}
      <div className="flex-1 flex flex-col">
        {/* HEADER */}
        <div className="p-4 border-b bg-white flex items-center gap-2">
          <Brain className="text-blue-500" />
          <h2 className="font-semibold">AI Tutor</h2>
          <div className="ml-auto">
            <span className="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded-full">
              📚 Grounded in NCERT
            </span>
          </div>
        </div>

        {/* CHAT AREA */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {messages.map((msg, i) => (
            <div
              key={i}
              className={`max-w-lg p-4 rounded-2xl text-sm leading-relaxed ${
                msg.role === "user"
                  ? "ml-auto bg-blue-500 text-white rounded-br-sm"
                  : "bg-white border text-gray-800 rounded-bl-sm shadow-sm"
              }`}
            >
              {msg.text}
            </div>
          ))}
          <div ref={bottomRef} />
        </div>

        {/* INPUT */}
        <div className="p-4 border-t bg-white flex gap-2">
          <input
            type="text"
            placeholder="Ask anything... (Enter to send)"
            className="flex-1 px-4 py-2.5 rounded-xl border outline-none text-sm focus:border-blue-400 transition"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={streaming}
          />
          <button
            onClick={handleSend}
            disabled={streaming || !input.trim()}
            className="bg-blue-500 text-white p-3 rounded-xl hover:bg-blue-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Send size={18} />
          </button>
        </div>
      </div>
    </div>
  );
}
