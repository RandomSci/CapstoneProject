from dependencies.session import get_current_user
from fastapi.staticfiles import StaticFiles
from starlette.middleware.base import BaseHTTPMiddleware
from fastapi import APIRouter, FastAPI, Response, Depends, Form, HTTPException, status, File, UploadFile
from pydantic import BaseModel
from fastapi.responses import StreamingResponse, HTMLResponse, RedirectResponse, JSONResponse, FileResponse
from fastapi.templating import Jinja2Templates
from starlette.requests import Request
from fastapi.middleware.cors import CORSMiddleware
from pathlib import Path as FilePath
from fastapi import *
from ultralytics import YOLO
from typing import Optional, Dict, List
import cv2, matplotlib.pyplot as plt, pandas as pd, torch, bcrypt, user_agents, datetime
from datetime import date, time, timedelta
import traceback
from contextlib import asynccontextmanager
import uvicorn, secrets, qrcode, io, socket, time, shutil
import json
import aiofiles
import mediapipe as mp
import time
import subprocess
import os
import numpy as np
import stat
import threading
from threading import Event

video_processing_threads = {}
stop_events = {}

class ExerciseVideoSubmissionResponse(BaseModel):
    submission_id: int
    status: str
    message: Optional[str] = None
class ExerciseProgressRequest(BaseModel):
    sets_completed: int
    repetitions_completed: Optional[int] = None
    duration_seconds: Optional[int] = None
    pain_level: Optional[int] = None
    difficulty_level: Optional[int] = None
    notes: Optional[str] = None

class AppointmentRequest(BaseModel):
    therapist_id: int
    date: str
    time: str
    type: str
    notes: str = None
    insuranceProvider: str = None
    insuranceMemberId: str = None

class AppointmentResponse(BaseModel):
    status: str  
    reason: Optional[str] = None 


class AppointmentRequestListItem(BaseModel):
    request_id: int
    date: str
    time: str
    status: str
    user_name: str
    notes: Optional[str] = None
class MessageRequest(BaseModel):
    recipient_id: int
    recipient_type: str = "therapist"
    subject: str
    content: str
    
    
class Register(BaseModel):
    username: str
    email: str
    password: str

class TherapistRegister(BaseModel):
    first_name: str
    last_name: str
    company_email: str
    password: str
class Login(BaseModel):
    username: str
    password: str
    remember_me: bool = False
    
class SessionData(BaseModel):
    user_id: int
    email: str
    expires: datetime.datetime

class User_Data(BaseModel):
    username: str
    email: str
    joined: str

async def create_session(user_id: int, email: str, remember: bool = False) -> str:
    session_id = secrets.token_hex(16)
  

def serialize_document(doc):
    """Convert MongoDB document to a JSON-serializable format"""
    return {
        "id": str(doc["_id"]),
        "user_id": doc["user_id"],
        "image": doc["image"],
        "annotations": doc["annotations"],
        "size": doc["size"],
        "save_location": doc["save_location"],
        "model_used": doc["model_used"],
        "timestamp": doc["timestamp"].isoformat(),
        "status": doc["status"],
        "confidence_threshold": doc["confidence_threshold"],
        "processing_time": doc["processing_time"],
        "device": doc["device"]
    }
    
