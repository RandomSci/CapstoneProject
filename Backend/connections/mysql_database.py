import mysql.connector
from mysql.connector import Error
from mysql.connector.cursor import MySQLCursorDict
from connections.functions import *
import os
import time
import bcrypt
from fastapi import HTTPException

def get_Mysql_db():
    try:
        host = os.getenv("MYSQL_HOST", "mysql.railway.internal")
        port = int(os.getenv("MYSQL_PORT", 3306))
        user = os.getenv("MYSQL_USER", "root")
        password = os.getenv("MYSQL_PASSWORD", "zgOcgtuHZLmHfTBxpxAgCaEzgeVnOEII")
        database = os.getenv("MYSQL_DB", "railway")
        
        connection = mysql.connector.connect(
            host=host,
            port=port,
            user=user,
            password=password,
            database=database
        )
        return connection
    except Exception as e:
        print(f"Database connection failed: {e}")
        raise

def Register_User_Web(first_name, last_name, company_email, password):
    db = get_Mysql_db()
    cursor = db.cursor(dictionary=True)
    hashed_password = bcrypt.hashpw(password.password.encode("utf-8"), bcrypt.gensalt())
    try:
        cursor.execute("SELECT COUNT(*) AS count FROM Therapists WHERE first_name = %s AND last_name = %s", (first_name, last_name))
        count = cursor.fetchone()['count']   
        if count > 0:
            raise HTTPException(status_code=400, detail="Username or email already exists.")
        cursor.execute(
            "INSERT INTO Therapists (first_name, last_name, company_email, password) VALUES (%s, %s, %s, %s)",
            (first_name, last_name, company_email, hashed_password.decode("utf-8"))
        )
        db.commit()
        return {"message": "User registered successfully"}
    except mysql.connector.errors.IntegrityError:  
        return {"error": "Username or email already exists."}
    finally:
        cursor.close()
        db.close()
        
async def get_exercise_categories():
    db = get_Mysql_db()
    cursor = None
    
    try:
        cursor = db.cursor(dictionary=True)
        cursor.execute("SELECT * FROM ExerciseCategories ORDER BY name")
        return cursor.fetchall()
    except Exception as e:
        print(f"Error fetching exercise categories: {e}")
        return []
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()
            
async def user_profile(user_id):
    db = get_Mysql_db()
    cursor = None
    try:
        cursor = db.cursor(dictionary=True)
        cursor.execute("SELECT * FROM users WHERE user_id = %s", (user_id,))
        return cursor.fetchall()
    except Exception as e:
        print(f"Error fetching patient profile: {e}")
        return []
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()

async def user_patient_profile(user_id):
    db = get_Mysql_db()
    cursor = None
    try:
        cursor = db.cursor(dictionary=True)
        cursor.execute("SELECT * FROM Patients WHERE user_id = %s", (user_id,))
        return cursor.fetchall()
    except Exception as e:
        print(f"Error fetching patient profile: {e}")
        return []
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()

async def get_therapist_data(therapist_id):
    db = get_Mysql_db()
    cursor = None
    try:
        cursor = db.cursor(dictionary=True)
        cursor.execute("SELECT * FROM Therapists WHERE id = %s", (therapist_id,))
        return cursor.fetchall()
    except Exception as e:
        print(f"Error fetching therapist profile: {e}")
        return []
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()

async def get_appointment_data(patient_id):
    db = get_Mysql_db()
    cursor = None
    try:
        cursor = db.cursor(dictionary=True)
        cursor.execute("SELECT * FROM Appointments WHERE patient_id = %s", (patient_id,))
        return cursor.fetchall()
    except Exception as e:
        print(f"Error fetching appointment data: {e}")
        return []
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()

async def get_treatment_plans(patient_id):
    db = get_Mysql_db()
    cursor = None
    try:
        cursor = db.cursor(dictionary=True)
        cursor.execute("SELECT * FROM TreatmentPlans WHERE patient_id = %s", (patient_id,))
        return cursor.fetchall()
    except Exception as e:
        print(f"Error fetching treatment plans: {e}")
        return []
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()

async def get_treatment_plan_exercises(plan_id):
    db = get_Mysql_db()
    cursor = None
    try:
        cursor = db.cursor(dictionary=True)
        cursor.execute("""
            SELECT * FROM TreatmentPlanExercises 
            WHERE plan_id = %s
        """, (plan_id,))
        exercises = cursor.fetchall()
        
        for exercise in exercises:
            cursor.execute("""
                SELECT * FROM Exercises 
                WHERE exercise_id = %s
            """, (exercise['exercise_id'],))
            exercise_details = cursor.fetchone()
            if exercise_details:
                exercise['exercise_details'] = exercise_details
        
        return exercises
    except Exception as e:
        print(f"Error fetching treatment plan exercises: {e}")
        return []
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()
            
async def get_exercise_details(exercise_id):
    db = get_Mysql_db()
    cursor = None
    try:
        cursor = db.cursor(dictionary=True)
        cursor.execute("SELECT * FROM Exercises WHERE exercise_id = %s", (exercise_id,))
        return cursor.fetchone()
    except Exception as e:
        print(f"Error fetching exercise details: {e}")
        return None
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()