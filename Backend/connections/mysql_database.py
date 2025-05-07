import pymysql
import pymysql.cursors
from connections.functions import *
import os
import time
import bcrypt
from fastapi import HTTPException
import logging

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger("database")

def get_Mysql_db():
    try:
        host = os.getenv("MYSQL_HOST", "mysql.railway.internal")
        port = int(os.getenv("MYSQL_PORT", 3306))
        user = os.getenv("MYSQL_USER", "root")
        password = os.getenv("MYSQL_PASSWORD", "zgOcgtuHZLmHfTBxpxAgCaEzgeVnOEII")
        database = os.getenv("MYSQL_DB", "railway")
        
        logger.debug(f"Connecting to MySQL at {host}:{port}")
        
        connection = pymysql.connect(
            host=host,
            port=port,
            user=user,
            password=password,
            database=database
        )
        
        logger.debug("Connection successful")
        return connection
    except Exception as e:
        logger.error(f"Database connection failed: {e}", exc_info=True)
        raise

def Register_User_Web(first_name, last_name, company_email, password):
    db = get_Mysql_db()
    cursor = db.cursor(pymysql.cursors.DictCursor)
    hashed_password = bcrypt.hashpw(password.password.encode("utf-8"), bcrypt.gensalt())
    try:
        logger.debug(f"Registering user: {first_name} {last_name}, {company_email}")
        cursor.execute("SELECT COUNT(*) AS count FROM Therapists WHERE first_name = %s AND last_name = %s", (first_name, last_name))
        result = cursor.fetchone()
        count = result.get('count', result.get('COUNT(*)', 0))
        
        if count > 0:
            logger.warning(f"User already exists: {first_name} {last_name}")
            raise HTTPException(status_code=400, detail="Username or email already exists.")
            
        cursor.execute(
            "INSERT INTO Therapists (first_name, last_name, company_email, password) VALUES (%s, %s, %s, %s)",
            (first_name, last_name, company_email, hashed_password.decode("utf-8"))
        )
        db.commit()
        logger.debug("User registered successfully")
        return {"message": "User registered successfully"}
    except pymysql.err.IntegrityError:
        logger.error("Integrity error during registration")
        return {"error": "Username or email already exists."}
    except Exception as e:
        logger.error(f"Registration error: {e}", exc_info=True)
        return {"error": f"Registration failed: {str(e)}"}
    finally:
        cursor.close()
        db.close()
        
async def get_exercise_categories():
    db = get_Mysql_db()
    cursor = None
    
    try:
        cursor = db.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT * FROM ExerciseCategories ORDER BY name")
        return cursor.fetchall()
    except Exception as e:
        logger.error(f"Error fetching exercise categories: {e}", exc_info=True)
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
        cursor = db.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT * FROM users WHERE user_id = %s", (user_id,))
        return cursor.fetchall()
    except Exception as e:
        logger.error(f"Error fetching patient profile: {e}", exc_info=True)
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
        cursor = db.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT * FROM Patients WHERE user_id = %s", (user_id,))
        return cursor.fetchall()
    except Exception as e:
        logger.error(f"Error fetching patient profile: {e}", exc_info=True)
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
        cursor = db.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT * FROM Therapists WHERE id = %s", (therapist_id,))
        return cursor.fetchall()
    except Exception as e:
        logger.error(f"Error fetching therapist profile: {e}", exc_info=True)
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
        cursor = db.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT * FROM Appointments WHERE patient_id = %s", (patient_id,))
        return cursor.fetchall()
    except Exception as e:
        logger.error(f"Error fetching appointment data: {e}", exc_info=True)
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
        cursor = db.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT * FROM TreatmentPlans WHERE patient_id = %s", (patient_id,))
        return cursor.fetchall()
    except Exception as e:
        logger.error(f"Error fetching treatment plans: {e}", exc_info=True)
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
        cursor = db.cursor(pymysql.cursors.DictCursor)
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
        logger.error(f"Error fetching treatment plan exercises: {e}", exc_info=True)
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
        cursor = db.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT * FROM Exercises WHERE exercise_id = %s", (exercise_id,))
        return cursor.fetchone()
    except Exception as e:
        logger.error(f"Error fetching exercise details: {e}", exc_info=True)
        return None
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()

def verify_therapist_login(email, password):
    """
    Login function for therapists
    """
    db = None
    cursor = None
    try:
        logger.debug(f"Verifying login for: {email}")
        db = get_Mysql_db()
        cursor = db.cursor(pymysql.cursors.DictCursor)
        
        cursor.execute(
            "SELECT id, first_name, last_name, password FROM Therapists WHERE company_email = %s",
            (email,)
        )
        
        therapist = cursor.fetchone()
        logger.debug(f"Therapist found: {therapist is not None}")
        
        if not therapist:
            return None
            
        stored_password = therapist['password']
        if isinstance(stored_password, str):
            stored_password = stored_password.encode('utf-8')
            
        logger.debug("Checking password")
        if bcrypt.checkpw(password.encode('utf-8'), stored_password):
            logger.debug("Password verified")
            return {
                "user_id": therapist['id'],
                "first_name": therapist['first_name'],
                "last_name": therapist['last_name'],
                "email": email,
                "user_type": "therapist"
            }
        else:
            logger.warning("Password verification failed")
            return None
    except Exception as e:
        logger.error(f"Login verification error: {e}", exc_info=True)
        return None
    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()