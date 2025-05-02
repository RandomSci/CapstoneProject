from fastapi import Request, HTTPException, Depends
from connections.redis_database import get_redis_session

async def get_current_user(request: Request):
    session_id = request.cookies.get("session_id")

    if not session_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    session = await get_redis_session(session_id)
    print(f"session: {session}")

    if not session:
        raise HTTPException(status_code=401, detail="Session expired or invalid")

    return session  

