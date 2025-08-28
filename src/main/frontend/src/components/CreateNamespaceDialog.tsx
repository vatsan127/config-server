import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Alert,
  Box,
  Typography,
} from '@mui/material';

interface CreateNamespaceDialogProps {
  open: boolean;
  onClose: () => void;
  onCreateNamespace: (namespace: string) => Promise<void>;
}

const CreateNamespaceDialog: React.FC<CreateNamespaceDialogProps> = ({
  open,
  onClose,
  onCreateNamespace,
}) => {
  const [namespace, setNamespace] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleClose = () => {
    if (!loading) {
      setNamespace('');
      setError(null);
      onClose();
    }
  };

  const handleSubmit = async () => {
    if (!namespace.trim()) {
      setError('Namespace name is required');
      return;
    }

    // Basic validation for namespace name
    const namespaceRegex = /^[a-zA-Z0-9][a-zA-Z0-9-_]*$/;
    if (!namespaceRegex.test(namespace.trim())) {
      setError('Namespace name must start with alphanumeric character and can contain only letters, numbers, hyphens, and underscores');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await onCreateNamespace(namespace.trim());
      setNamespace('');
      // Dialog will be closed by parent component
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to create namespace');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && !loading) {
      handleSubmit();
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Create New Namespace</DialogTitle>
      <DialogContent>
        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Namespaces help organize configurations for different teams, projects, or environments.
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <TextField
          autoFocus
          margin="dense"
          label="Namespace Name"
          type="text"
          fullWidth
          variant="outlined"
          value={namespace}
          onChange={(e) => setNamespace(e.target.value)}
          onKeyPress={handleKeyPress}
          disabled={loading}
          placeholder="e.g., frontend, backend, staging"
          helperText="Use alphanumeric characters, hyphens, and underscores only"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Cancel
        </Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={loading || !namespace.trim()}
        >
          {loading ? 'Creating...' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default CreateNamespaceDialog;