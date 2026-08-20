export interface Setting {
  id: string;
  label: string;
}

export interface CharacterProfile {
  name: string;
  concept: string;
  setting: Setting;
}

export type ConsequenceTone = 'neutral' | 'good' | 'bad';

export interface Consequence {
  text: string;
  tone: ConsequenceTone;
}

export interface StoryNode {
  id: string;
  type: string;
  text: string;
  rollText: string;
  imagePrompt: string | null;
  consequences: Consequence[];
  createdAt: string;
}

export interface GameSession {
  id: string;
  character: CharacterProfile;
  language: 'de' | 'en';
  images: boolean;
  dead: boolean;
  turn: number;
  health: number;
  maxHealth: number;
  resolve: number;
  maxResolve: number;
  resources: Record<string, number>;
  inventory: string[];
  effects: string[];
  nodes: StoryNode[];
  createdAt: string;
}

export interface CreateSessionRequest {
  name: string;
  concept: string;
  setting: string;
}
