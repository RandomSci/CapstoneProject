import redis.asyncio as redis
import uuid
import os
from dotenv import load_dotenv
import traceback
import json, secrets, time
from urllib.parse import urlencode

load_dotenv()

REDIS_HOST = os.getenv("REDIS_HOST", "redis")  
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))

r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)
SESSION_TTL = 63072000

async def create_redis_session(data: dict):
    session_id = str(uuid.uuid4())
    session_key = f"session:{session_id}"  
    
    try:
        json_data = json.dumps(data)
        await r.set(session_key, json_data, ex=SESSION_TTL)
        
        verification = await r.get(session_key)
        print(f"Session verification: {verification}")
        
        return session_id
    except Exception as e:
        print(f"Error creating Redis session: {e}")
        return None
    
async def test_redis_connection():
    try:
        await r.set('test_key', 'Success!')
        value = await r.get('test_key')
        print(f"Test Redis connection successful: {value}")
        return True
    except Exception as e:
        print(f"Error connecting to Redis: {e}")
        return False

async def get_redis_session(session_id: str):
    """Retrieve session data from Redis asynchronously"""
    try:

        session_key = f"session:{session_id}"
        


        json_data = await r.get(session_key)
        
        if not json_data:
            print(f"Session ID {session_id} does not exist or has expired.")
            return None
        

        if isinstance(json_data, bytes):
            json_data = json_data.decode('utf-8')
        

        try:
            session_data = json.loads(json_data)
            print(f"Session data retrieved successfully: {session_data}")
            return session_data
        except json.JSONDecodeError as e:
            print(f"Error decoding JSON from Redis: {e}")
            print(f"Raw data: {json_data}")
            return None
            
    except Exception as e:
        print(f"Error retrieving Redis session: {e}")
        print(f"Traceback: {traceback.format_exc()}")
        return None
    
async def delete_redis_session(session_id: str):
    """
    >>> To log out user
    Args:
    session_id (str): User id
    """
    await r.delete(session_id)
    
async def generate_video_token(user_id, filename, expiry_seconds=3600):
    token = secrets.token_hex(16)
    
    token_data = {
        "user_id": user_id,
        "filename": filename,
        "expires": int(time.time()) + expiry_seconds
    }
    
    token_key = f"video_token:{token}"
    await r.set(token_key, json.dumps(token_data), ex=expiry_seconds)
    
    return token

async def verify_video_token(token, filename):

    token_data_str = await r.get(f"video_token:{token}")
    if not token_data_str:
        return None
    
    token_data = json.loads(token_data_str)
    

    if int(time.time()) > token_data["expires"] or token_data["filename"] != filename:
        return None
    
    return token_data["user_id"]