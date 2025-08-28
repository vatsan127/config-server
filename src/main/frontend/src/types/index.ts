export interface Payload {
  action?: string;
  appName?: string;
  namespace: string;
  path: string;
  email?: string;
  content?: string;
  message?: string;
  commitId?: string;
}

export interface CommitInfo {
  commitId: string;
  author: string;
  email: string;
  date: string;
  message: string;
}

export interface ConfigHistory {
  filePath: string;
  commits: CommitInfo[];
}

export interface CommitChanges {
  commitId: string;
  message: string;
  author: string;
  commitTime: Date;
  changes: string;
}

export interface ChangeEntry {
  namespace: string;
  filePath: string;
  action: string;
  timestamp: string;
  author: string;
  commitId: string;
  message: string;
}

export interface ApiResponse<T> {
  data: T;
  status: number;
}