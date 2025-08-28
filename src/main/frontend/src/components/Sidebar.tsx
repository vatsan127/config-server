import React from 'react';
import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Button,
  Typography,
  CircularProgress,
  Divider,
} from '@mui/material';
import {
  Folder as FolderIcon,
  Add as AddIcon,
  FolderOpen as FolderOpenIcon,
} from '@mui/icons-material';

interface SidebarProps {
  width: number;
  namespaces: string[];
  selectedNamespace: string | null;
  loading: boolean;
  onNamespaceSelect: (namespace: string) => void;
  onCreateClick: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({
  width,
  namespaces,
  selectedNamespace,
  loading,
  onNamespaceSelect,
  onCreateClick,
}) => {
  return (
    <Drawer
      variant="permanent"
      sx={{
        width,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width,
          boxSizing: 'border-box',
          bgcolor: 'grey.50',
        },
      }}
    >
      <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'white' }}>
        <Typography variant="h6" component="div">
          Namespaces
        </Typography>
      </Box>

      <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress size={24} />
          </Box>
        ) : (
          <List sx={{ pt: 0 }}>
            {namespaces.length === 0 ? (
              <ListItem>
                <ListItemText
                  primary="No namespaces found"
                  secondary="Create your first namespace below"
                  sx={{ textAlign: 'center', color: 'text.secondary' }}
                />
              </ListItem>
            ) : (
              namespaces.map((namespace) => (
                <ListItem key={namespace} disablePadding>
                  <ListItemButton
                    selected={selectedNamespace === namespace}
                    onClick={() => onNamespaceSelect(namespace)}
                    sx={{
                      '&.Mui-selected': {
                        bgcolor: 'primary.50',
                        '&:hover': {
                          bgcolor: 'primary.100',
                        },
                      },
                    }}
                  >
                    <ListItemIcon>
                      {selectedNamespace === namespace ? (
                        <FolderOpenIcon color="primary" />
                      ) : (
                        <FolderIcon />
                      )}
                    </ListItemIcon>
                    <ListItemText
                      primary={namespace}
                      primaryTypographyProps={{
                        fontWeight: selectedNamespace === namespace ? 'medium' : 'normal',
                      }}
                    />
                  </ListItemButton>
                </ListItem>
              ))
            )}
          </List>
        )}
      </Box>

      <Divider />
      <Box sx={{ p: 2 }}>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={onCreateClick}
          fullWidth
          disabled={loading}
        >
          Create Namespace
        </Button>
      </Box>
    </Drawer>
  );
};

export default Sidebar;