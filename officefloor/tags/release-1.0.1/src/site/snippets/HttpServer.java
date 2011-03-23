// OfficeFloor source implementation to simplify creating a HTTP Server
HttpServerAutoWireOfficeFloorSource server = new HttpServerAutoWireOfficeFloorSource();
        
// Add dynamic web page
server.addHttpTemplate("example.ofp", Example.class, "example");
        
// Add configured DataSource for dependency injection
server.addManagedObject(DataSourceManagedObjectSource.class, null, DataSource.class).loadProperties("datasource.properties");
        
// Assign Team (specific thread pool) responsible for executing tasks with a DataSource dependency
server.assignTeam(LeaderFollowerTeamSource.class, DataSource.class).addProperty("size", "10");
        
// Start the HTTP Server
server.openOfficeFloor();