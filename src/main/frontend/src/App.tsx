import React, { useState, useEffect } from 'react';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Button,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  Folder as FolderIcon,
  Add as AddIcon,
  Storage as StorageIcon,
} from '@mui/icons-material';
import { ConfigServerApi } from './services/api';
import Sidebar from './components/Sidebar';
import MainArea from './components/MainArea';
import CreateNamespaceDialog from './components/CreateNamespaceDialog';

const DRAWER_WIDTH = 280;

interface AppState {
  namespaces: string[];
  selectedNamespace: string | null;
  loading: boolean;
  error: string | null;
  createDialogOpen: boolean;
}

function App() {
  const [state, setState] = useState<AppState>({
    namespaces: [],
    selectedNamespace: null,
    loading: true,
    error: null,
    createDialogOpen: false,
  });

  const loadNamespaces = async () => {
    try {
      setState(prev => ({ ...prev, loading: true, error: null }));
      const namespaces = await ConfigServerApi.listNamespaces();
      setState(prev => ({ ...prev, namespaces, loading: false }));
    } catch (error) {
      setState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to load namespaces',
        loading: false,
      }));
    }
  };

  useEffect(() => {
    loadNamespaces();
  }, []);

  const handleNamespaceSelect = (namespace: string) => {
    setState(prev => ({ ...prev, selectedNamespace: namespace }));
  };

  const handleCreateNamespace = async (namespace: string) => {
    try {
      await ConfigServerApi.createNamespace(namespace);
      setState(prev => ({ ...prev, createDialogOpen: false }));
      await loadNamespaces(); // Reload namespaces
    } catch (error) {
      throw error; // Let dialog handle the error
    }
  };

  const handleCloseError = () => {
    setState(prev => ({ ...prev, error: null }));
  };

  return (
    <Box sx={{ display: 'flex' }}>
      {/* App Bar */}
      <AppBar
        position="fixed"
        sx={{
          width: `calc(100% - ${DRAWER_WIDTH}px)`,
          ml: `${DRAWER_WIDTH}px`,
        }}
      >
        <Toolbar>
          <StorageIcon sx={{ mr: 2 }} />
          <Typography variant="h6" noWrap component="div">
            Config Server
          </Typography>
        </Toolbar>
      </AppBar>

      {/* Sidebar */}
      <Sidebar
        width={DRAWER_WIDTH}
        namespaces={state.namespaces}
        selectedNamespace={state.selectedNamespace}
        loading={state.loading}
        onNamespaceSelect={handleNamespaceSelect}
        onCreateClick={() => setState(prev => ({ ...prev, createDialogOpen: true }))}
      />

      {/* Main Content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          bgcolor: 'background.default',
          p: 3,
          width: `calc(100% - ${DRAWER_WIDTH}px)`,
        }}
      >
        <Toolbar />
        
        {/* Error Alert */}
        {state.error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={handleCloseError}>
            {state.error}
          </Alert>
        )}

        {/* Main Area */}
        <MainArea selectedNamespace={state.selectedNamespace} />
      </Box>

      {/* Create Namespace Dialog */}
      <CreateNamespaceDialog
        open={state.createDialogOpen}
        onClose={() => setState(prev => ({ ...prev, createDialogOpen: false }))}
        onCreateNamespace={handleCreateNamespace}
      />
    </Box>
  );
}

export default App;