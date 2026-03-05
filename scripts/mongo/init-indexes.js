db = db.getSiblingDB('ai_gateway');

// api_keys indexes
db.api_keys.createIndex({ "key": 1 }, { unique: true });
db.api_keys.createIndex({ "teamId": 1 });
db.api_keys.createIndex({ "enabled": 1 });

// request_logs indexes (TTL: 30 days)
db.request_logs.createIndex({ "createdAt": 1 }, { expireAfterSeconds: 2592000 });
db.request_logs.createIndex({ "apiKeyId": 1, "createdAt": -1 });
db.request_logs.createIndex({ "teamId": 1, "createdAt": -1 });
db.request_logs.createIndex({ "model": 1, "createdAt": -1 });
db.request_logs.createIndex({ "status": 1 });

// model_configs indexes
db.model_configs.createIndex({ "modelAlias": 1 }, { unique: true });
db.model_configs.createIndex({ "provider": 1 });
db.model_configs.createIndex({ "enabled": 1 });

// routing_rules indexes
db.routing_rules.createIndex({ "priority": 1 });
db.routing_rules.createIndex({ "enabled": 1 });

// usage_stats indexes
db.usage_stats.createIndex({ "teamId": 1, "date": -1 });
db.usage_stats.createIndex({ "model": 1, "date": -1 });

print('AI Gateway indexes created successfully');
