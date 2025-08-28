import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Box,
  Typography,
  Alert,
  Grid,
  Paper,
} from '@mui/material';
import { ConfigServerApi } from '../services/api';
import { Payload } from '../types';

interface ConfigEditorProps {
  open: boolean;
  onClose: () => void;
  namespace: string;
  filePath?: string;
  appName?: string;
  mode: 'create' | 'edit';
  onSuccess?: () => void;
}

interface EditorState {
  appName: string;
  email: string;
  content: string;
  commitMessage: string;
  loading: boolean;
  error: string | null;
}

const ConfigEditor: React.FC<ConfigEditorProps> = ({
  open,
  onClose,
  namespace,
  filePath = '/',
  appName = '',
  mode,
  onSuccess,
}) => {
  const [state, setState] = useState<EditorState>({
    appName: appName || '',
    email: '',
    content: '',
    commitMessage: mode === 'create' ? 'Initial configuration' : 'Update configuration',
    loading: false,
    error: null,
  });

  useEffect(() => {
    if (open && mode === 'edit' && filePath) {
      loadConfigFile();
    } else if (open && mode === 'create') {
      // Reset form for create mode
      setState(prev => ({
        ...prev,
        appName: appName || '',
        email: '',
        content: '',
        commitMessage: 'Initial configuration',
        error: null,
      }));
    }
  }, [open, mode, filePath, appName]);

  const loadConfigFile = async () => {
    try {
      setState(prev => ({ ...prev, loading: true, error: null }));
      const payload: Payload = {
        namespace,
        path: filePath,
        email: 'temp@example.com', // Required by API but not used for fetch
      };
      const result = await ConfigServerApi.fetchConfig(payload);
      setState(prev => ({
        ...prev,
        content: result.content || '',
        loading: false,
      }));
    } catch (error) {
      setState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to load configuration',
        loading: false,
      }));
    }
  };

  const handleSave = async () => {
    if (!state.email.trim()) {
      setState(prev => ({ ...prev, error: 'Email is required' }));
      return;
    }

    if (mode === 'create' && !state.appName.trim()) {
      setState(prev => ({ ...prev, error: 'App name is required' }));
      return;
    }

    try {
      setState(prev => ({ ...prev, loading: true, error: null }));

      const payload: Payload = {
        namespace,
        path: filePath,
        email: state.email,
        appName: state.appName,
        content: state.content,
        message: state.commitMessage,
      };

      if (mode === 'create') {
        await ConfigServerApi.createConfig(payload);
      } else {
        await ConfigServerApi.updateConfig(payload);
      }

      onSuccess?.();
      handleClose();
    } catch (error) {
      setState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to save configuration',
        loading: false,
      }));
    }
  };

  const handleClose = () => {
    if (!state.loading) {
      setState({
        appName: '',
        email: '',
        content: '',
        commitMessage: mode === 'create' ? 'Initial configuration' : 'Update configuration',
        loading: false,
        error: null,
      });
      onClose();
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>
        {mode === 'create' ? 'Create Configuration' : 'Edit Configuration'}
      </DialogTitle>
      
      <DialogContent>
        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Namespace: <strong>{namespace}</strong> | Path: <strong>{filePath}</strong>
          </Typography>
        </Box>

        {state.error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {state.error}
          </Alert>
        )}

        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Email"
              type="email"
              value={state.email}
              onChange={(e) => setState(prev => ({ ...prev, email: e.target.value }))}
              disabled={state.loading}
              required
              helperText="Your email for git commits"
              margin="dense"
            />
          </Grid>
          
          {mode === 'create' && (
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Application Name"
                value={state.appName}
                onChange={(e) => setState(prev => ({ ...prev, appName: e.target.value }))}
                disabled={state.loading}
                required
                helperText="Name of the application"
                margin="dense"
              />
            </Grid>
          )}

          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Commit Message"
              value={state.commitMessage}
              onChange={(e) => setState(prev => ({ ...prev, commitMessage: e.target.value }))}
              disabled={state.loading}
              margin="dense"
            />
          </Grid>

          <Grid item xs={12}>
            <Paper variant="outlined" sx={{ p: 1 }}>
              <Typography variant="subtitle2" gutterBottom>
                Configuration Content (YAML)
              </Typography>
              <TextField
                fullWidth
                multiline
                rows={12}
                value={state.content}
                onChange={(e) => setState(prev => ({ ...prev, content: e.target.value }))}
                disabled={state.loading}
                placeholder={mode === 'create' ? 
                  "server:\n  port: 8080\n  servlet:\n    context-path: /my-app\n\nspring:\n  application:\n    name: my-app" :
                  "Loading..."
                }
                sx={{
                  '& .MuiInputBase-input': {
                    fontFamily: 'monospace',
                    fontSize: '0.875rem',
                  },
                }}
              />
            </Paper>
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions>
        <Button onClick={handleClose} disabled={state.loading}>
          Cancel
        </Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={state.loading || !state.email.trim() || (mode === 'create' && !state.appName.trim())}
        >
          {state.loading ? 'Saving...' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ConfigEditor;