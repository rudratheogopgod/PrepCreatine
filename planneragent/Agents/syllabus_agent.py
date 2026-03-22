from __future__ import annotations
import os
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_community.tools import DuckDuckGoSearchRun
from langchain_groq import ChatGroq
from state import AgentState
from dotenv import load_dotenv

load_dotenv()

LLM = ChatGroq(api_key=os.getenv("API_GROQ"), model="llama-3.3-70b-versatile", temperature=0)

def syllabus_agent(state: AgentState) -> dict:
    """
    Searches the web for the detailed exam syllabus and summarizes it.
    """
    exam_name = state.get("exam_name", "UPSC")
    
    search = DuckDuckGoSearchRun()
    query = f"{exam_name} detailed syllabus topics and subjects 2025"
    
    try:
        raw_results = search.invoke(query)
    except Exception as e:
        raw_results = f"Search failed: {e}"

    system_prompt = "You are an expert curriculum organizer. Extract and organize the detailed syllabus from the provided web search results. Output it clearly grouped by subject."
    user_prompt = f"Exam: {exam_name}\n\nSearch Results:\n{raw_results}\n\nPlease organize these results into a comprehensive syllabus outline. If the search results are poor, rely on your internal knowledge of the {exam_name} syllabus."
    
    response = LLM.invoke([
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_prompt),
    ])
    
    syllabus = response.content.strip()
    
    return {
        "syllabus": syllabus,
        "messages": [f"🔍 Syllabus Agent: Retrieved and summarized syllabus for {exam_name} via web search."]
    }
