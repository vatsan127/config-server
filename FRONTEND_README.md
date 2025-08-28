# Config Server Frontend

A React TypeScript frontend for the Config Server with Material UI.

## Features

- **Namespace Management**: Create and browse namespaces from the sidebar
- **File Management**: Browse directories and configuration files
- **Configuration Editor**: Create and edit YAML configuration files
- **Search**: Search through files in the current directory
- **Responsive Design**: Clean Material UI interface

## Build and Run

### Development Mode

1. **Start the Spring Boot backend**:
   ```bash
   mvn spring-boot:run
   ```

2. **Start the React frontend** (in a separate terminal):
   ```bash
   cd src/main/frontend
   npm install
   npm start
   ```

   The frontend will be available at http://localhost:3000 and will proxy API requests to the backend at http://localhost:8080.

### Production Build

The frontend is automatically built and included in the Spring Boot JAR:

```bash
mvn clean package
java -jar target/config-server-0.0.1-SNAPSHOT.jar
```

The complete application (backend + frontend) will be available at http://localhost:8080/config-server

## UI Layout

- **Sidebar**: Lists all namespaces with a "Create Namespace" button at the bottom
- **Main Area**: 
  - Shows welcome banner when no namespace is selected
  - Shows directory contents when a namespace is selected
  - Includes search functionality and breadcrumb navigation
- **Configuration Editor**: Modal dialog for creating and editing YAML files

## Usage

1. Create a namespace using the "Create Namespace" button
2. Select the namespace from the sidebar
3. Browse directories and files in the main area
4. Click "+" to create new configuration files
5. Click on existing files to edit them
6. Use the search box to filter files

## API Integration

The frontend integrates with these Spring Boot API endpoints:
- `POST /config-server/namespace/create` - Create namespace
- `POST /config-server/namespace/list` - List namespaces  
- `POST /config-server/namespace/files` - List directory contents
- `POST /config-server/config/create` - Create configuration file
- `POST /config-server/config/fetch` - Fetch configuration content
- `POST /config-server/config/update` - Update configuration file