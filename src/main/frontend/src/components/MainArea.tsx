import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Paper,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Breadcrumbs,
  Link,
  CircularProgress,
  Alert,
  IconButton,
  Tooltip,
  Chip,
  TextField,
  InputAdornment,
} from '@mui/material';
import {
  Settings as SettingsIcon,
  Folder as FolderIcon,
  InsertDriveFile as FileIcon,
  Home as HomeIcon,
  Add as AddIcon,
  Search as SearchIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { ConfigServerApi } from '../services/api';
import ConfigEditor from './ConfigEditor';

interface MainAreaProps {
  selectedNamespace: string | null;
}

interface MainAreaState {
  currentPath: string;
  files: string[];
  filteredFiles: string[];
  loading: boolean;
  error: string | null;
  searchTerm: string;
  editorOpen: boolean;
  editorMode: 'create' | 'edit';
  selectedFile: string | null;
}

const MainArea: React.FC<MainAreaProps> = ({ selectedNamespace }) => {
  const [state, setState] = useState<MainAreaState>({
    currentPath: '/',
    files: [],
    filteredFiles: [],
    loading: false,
    error: null,
    searchTerm: '',
    editorOpen: false,
    editorMode: 'create',
    selectedFile: null,
  });

  const loadDirectoryContents = async (namespace: string, path: string = '/') => {
    try {
      setState(prev => ({ ...prev, loading: true, error: null }));
      const files = await ConfigServerApi.listDirectoryContents(namespace, path);
      setState(prev => ({ 
        ...prev, 
        files, 
        filteredFiles: files,
        loading: false, 
        currentPath: path,
        searchTerm: '',
        editorOpen: false,
        selectedFile: null,
      }));
    } catch (error) {
      setState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to load directory contents',
        loading: false,
      }));
    }
  };

  const handleSearch = (searchTerm: string) => {
    setState(prev => ({
      ...prev,
      searchTerm,
      filteredFiles: prev.files.filter(file =>
        file.toLowerCase().includes(searchTerm.toLowerCase())
      ),
    }));
  };

  useEffect(() => {
    if (selectedNamespace) {
      setState({ 
        currentPath: '/', 
        files: [], 
        filteredFiles: [],
        loading: false, 
        error: null, 
        searchTerm: '',
        editorOpen: false,
        editorMode: 'create',
        selectedFile: null,
      });
      loadDirectoryContents(selectedNamespace, '/');
    }
  }, [selectedNamespace]);

  const handleFileClick = (fileName: string) => {
    if (fileName.endsWith('/')) {
      // It's a folder
      const newPath = state.currentPath === '/' 
        ? fileName 
        : `${state.currentPath}/${fileName}`;
      loadDirectoryContents(selectedNamespace!, newPath);
    } else {
      // It's a file - open for editing
      setState(prev => ({
        ...prev,
        editorOpen: true,
        editorMode: 'edit',
        selectedFile: fileName,
      }));
    }
  };

  const handleBreadcrumbClick = (path: string) => {
    loadDirectoryContents(selectedNamespace!, path);
  };

  const renderBreadcrumbs = () => {
    if (!selectedNamespace || state.currentPath === '/') {
      return (
        <Breadcrumbs>
          <Chip icon={<HomeIcon />} label={selectedNamespace || 'Home'} variant="outlined" />
        </Breadcrumbs>
      );
    }

    const pathParts = state.currentPath.split('/').filter(Boolean);
    const breadcrumbs = [
      <Link
        key="home"
        component="button"
        variant="inherit"
        onClick={() => handleBreadcrumbClick('/')}
        sx={{ display: 'flex', alignItems: 'center', textDecoration: 'none' }}
      >
        <HomeIcon sx={{ mr: 0.5 }} fontSize="inherit" />
        {selectedNamespace}
      </Link>
    ];

    pathParts.forEach((part, index) => {
      const path = '/' + pathParts.slice(0, index + 1).join('/');
      breadcrumbs.push(
        <Link
          key={path}
          component="button"
          variant="inherit"
          onClick={() => handleBreadcrumbClick(path)}
          sx={{ textDecoration: 'none' }}
        >
          {part.replace('/', '')}
        </Link>
      );
    });

    return <Breadcrumbs>{breadcrumbs}</Breadcrumbs>;
  };

  const WelcomeBanner = () => (
    <Card sx={{ mb: 3, bgcolor: 'primary.50', borderLeft: '4px solid', borderColor: 'primary.main' }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <SettingsIcon color="primary" sx={{ mr: 2, fontSize: 32 }} />
          <Typography variant="h4" component="h1" color="primary.main">
            Welcome to Config Server
          </Typography>
        </Box>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
          Git-based Configuration Management System
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} md={4}>
            <Box sx={{ p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
              <Typography variant="h6" gutterBottom>
                📁 Multi-Namespace Support
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Organize configurations across different teams and projects with isolated namespaces.
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={12} md={4}>
            <Box sx={{ p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
              <Typography variant="h6" gutterBottom>
                🔄 Version Control
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Full Git-based version control with commit history and change tracking.
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={12} md={4}>
            <Box sx={{ p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
              <Typography variant="h6" gutterBottom>
                ⚡ Performance
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Cached operations for fast configuration retrieval and management.
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );

  if (!selectedNamespace) {
    return <WelcomeBanner />;
  }

  return (
    <Box>
      {/* Navigation Bar with Search */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        mb: 2,
        gap: 2,
      }}>
        <Box sx={{ flex: 1 }}>
          {renderBreadcrumbs()}
        </Box>
        
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <TextField
            size="small"
            placeholder="Search files..."
            value={state.searchTerm}
            onChange={(e) => handleSearch(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
            sx={{ minWidth: 200 }}
          />
          
          <Tooltip title="Add Configuration">
            <IconButton 
              color="primary"
              onClick={() => setState(prev => ({
                ...prev,
                editorOpen: true,
                editorMode: 'create',
                selectedFile: null,
              }))}
            >
              <AddIcon />
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {state.error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {state.error}
        </Alert>
      )}

      <Paper>
        {state.loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : state.filteredFiles.length === 0 ? (
          <Box sx={{ p: 4, textAlign: 'center' }}>
            <Typography variant="h6" color="text.secondary" gutterBottom>
              {state.searchTerm ? 'No files found matching your search' : 'No files found'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {state.searchTerm 
                ? `Try adjusting your search term: "${state.searchTerm}"`
                : 'This directory is empty. Create your first configuration file.'
              }
            </Typography>
          </Box>
        ) : (
          <List>
            {state.filteredFiles.map((file, index) => {
              const isFolder = file.endsWith('/');
              return (
                <ListItem key={index} disablePadding>
                  <ListItemButton onClick={() => handleFileClick(file)}>
                    <ListItemIcon>
                      {isFolder ? <FolderIcon color="primary" /> : <FileIcon />}
                    </ListItemIcon>
                    <ListItemText
                      primary={isFolder ? file.slice(0, -1) : file}
                      secondary={isFolder ? 'Folder' : 'Configuration File'}
                    />
                    {!isFolder && (
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          setState(prev => ({
                            ...prev,
                            editorOpen: true,
                            editorMode: 'edit',
                            selectedFile: file,
                          }));
                        }}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                    )}
                  </ListItemButton>
                </ListItem>
              );
            })}
          </List>
        )}
      </Paper>

      {/* Config Editor Dialog */}
      <ConfigEditor
        open={state.editorOpen}
        onClose={() => setState(prev => ({ ...prev, editorOpen: false, selectedFile: null }))}
        namespace={selectedNamespace}
        filePath={state.selectedFile ? `${state.currentPath}${state.selectedFile}.yml` : `${state.currentPath}new-config.yml`}
        appName={state.selectedFile || 'my-app'}
        mode={state.editorMode}
        onSuccess={() => {
          // Refresh directory contents after successful save
          loadDirectoryContents(selectedNamespace!, state.currentPath);
        }}
      />
    </Box>
  );
};

export default MainArea;