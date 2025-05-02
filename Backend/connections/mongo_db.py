from pymongo import MongoClient
import os
import time

def get_Mongo_db(collection_name, max_retries=5, retry_delay=2):
    MONGO_HOST = os.getenv("MONGO_HOST", "mongodb")  
    MONGO_PORT = os.getenv("MONGO_PORT", "27017")
    MONGO_URI = f"mongodb://{MONGO_HOST}:{MONGO_PORT}"
    DB_NAME = "PerceptronX"
    COLLECTION_NAME = collection_name
    
    for attempt in range(max_retries):
        try:
            client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
            client.admin.command('ping')
            db = client[DB_NAME]
            collection = db[COLLECTION_NAME]
            return collection
        except Exception as e:
            if attempt < max_retries - 1:
                print(f"MongoDB connection attempt {attempt+1} failed: {e}. Retrying in {retry_delay} seconds...")
                time.sleep(retry_delay)
            else:
                print(f"Failed to connect to MongoDB after {max_retries} attempts: {e}")
                raise