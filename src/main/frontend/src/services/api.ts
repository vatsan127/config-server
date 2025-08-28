import axios, { AxiosResponse } from 'axios';
import { Payload, ConfigHistory, CommitChanges, ChangeEntry } from '../types';

const API_BASE = '/config-server';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

export class ConfigServerApi {
  // Namespace Management
  static async createNamespace(namespace: string): Promise<void> {
    await api.post('/namespace/create', { namespace });
  }

  static async listNamespaces(): Promise<string[]> {
    const response = await api.post('/namespace/list', {});
    return response.data;
  }

  static async listDirectoryContents(namespace: string, path: string = '/'): Promise<string[]> {
    const response = await api.post('/namespace/files', { namespace, path });
    return response.data;
  }

  // Configuration Management
  static async createConfig(payload: Payload): Promise<void> {
    await api.post('/config/create', { ...payload, action: 'create' });
  }

  static async fetchConfig(payload: Payload): Promise<Payload> {
    const response = await api.post('/config/fetch', { ...payload, action: 'fetch' });
    return response.data;
  }

  static async updateConfig(payload: Payload): Promise<void> {
    await api.post('/config/update', { ...payload, action: 'update' });
  }

  // Note: History and changelog APIs are available but not implemented in UI yet
  // static async getConfigHistory(payload: Payload): Promise<ConfigHistory>
  // static async getCommitChanges(payload: Payload): Promise<CommitChanges>  
  // static async getChangeLog(namespace: string): Promise<ChangeEntry[]>
}

// Error handling interceptor
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error) => {
    if (error.response) {
      // Server responded with error status
      const message = error.response.data?.message || error.response.statusText;
      throw new Error(`API Error: ${message}`);
    } else if (error.request) {
      // Request was made but no response received
      throw new Error('Network Error: No response from server');
    } else {
      // Something else happened
      throw new Error(`Request Error: ${error.message}`);
    }
  }
);