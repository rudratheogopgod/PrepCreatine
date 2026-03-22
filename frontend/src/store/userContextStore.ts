import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type StudyMode = 'in_depth' | 'revision' | 'overview' | 'speed_run'

export interface UserContextState {
  examType: string | null
  examDate: string | null
  studyMode: StudyMode
  dailyGoalMins: number
  weakTopics: string[]
  strongTopics: string[]
  customTopic: string | null
  theme: 'light' | 'dark' | 'system'
}

interface UserContextStore extends UserContextState {
  setContext: (ctx: Partial<UserContextState>) => void
  reset: () => void
}

const defaultState: UserContextState = {
  examType: null,
  examDate: null,
  studyMode: 'in_depth',
  dailyGoalMins: 90,
  weakTopics: [],
  strongTopics: [],
  customTopic: null,
  theme: 'system',
}

export const useUserContextStore = create<UserContextStore>()(
  persist(
    (set) => ({
      ...defaultState,
      setContext: (ctx) => set((s) => ({ ...s, ...ctx })),
      reset: () => set(defaultState),
    }),
    { name: 'prepcreatine-context' }
  )
)
