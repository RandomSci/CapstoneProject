import pymysql
import pymysql.cursors
from connections.functions import *
import os
import time
import bcrypt
from fastapi import HTTPException

class MySQLCompat:
    Error = pymysql.Error
    IntegrityError = pymysql.err.IntegrityError

pymysql.connector = MySQLCompat

def get_Mysql_db(max_retries=5, retry_delay=2):
    host = os.getenv("MYSQL_HOST", "mysql.railway.internal")
    user = os.getenv("MYSQL_USER", "root")
    password = os.getenv("MYSQL_PASSWORD", "yjjwIasbqHWwyKrbdmNRCWHVBGMgNMNG")
    database = os.getenv("MYSQL_DB", "perceptronx")
    port = int(os.getenv("MYSQL_PORT", "3306"))
    
    print(f"Connecting to MySQL at {host}:{port} as {user}")
    
    connection_configs = [
        {"ssl": True},
        {"ssl": {}},
        {},
        {"client_flag": pymysql.constants.CLIENT.SSL},
        {"ssl": {"ca": None}}
    ]
    
    for config in connection_configs:
        for attempt in range(max_retries // len(connection_configs) + 1):
            try:
                print(f"Trying connection config: {config}")
                
                connection_params = {
                    "host": host,
                    "user": user,
                    "password": password,
                    "database": database,
                    "port": port,
                    "cursorclass": pymysql.cursors.DictCursor,
                    "charset": 'utf8mb4'
                }
                
                connection_params.update(config)
                
                connection = pymysql.connect(**connection_params)
                print("MySQL connection successful!")
                return connection
            except Exception as err:
                print(f"Connection attempt failed with config {config}: {err}")
                if attempt < max_retries // len(connection_configs):
                    print(f"Retrying with same config in {retry_delay} seconds...")
                    time.sleep(retry_delay)
                else:
                    print(f"Moving to next configuration...")
                    break
    
    print("All connection configurations failed")
    raise Exception("Could not establish database connection after trying all configurations")

def Register_User_Web(first_name, last_name, company_email, password):
    db = get_Mysql_db()
    cursor = db.cursor()
    hashed_password = bcrypt.hashpw(password.password.encode("utf-8"), bcrypt.gensalt())
    try:
        cursor.execute("SELECT COUNT(*) FROM Therapists WHERE first_name = %s AND last_name = %s", (first_name, last_name))
        count = cursor.fetchone()['COUNT(*)']   
        if count > 0:
            raise HTTPException(status_code=400, detail="Username or email already exists.")
        cursor.execute(
            "INSERT INTO Therapists (first_name, last_name, company_email, password) VALUES (%s, %s, %s, %s)",
            (first_name, last_name, company_email, hashed_password.decode("utf-8"))
        )
        db.commit()
        return {"message": "User registered successfully"}
    except pymysql.err.IntegrityError:  
        return {"error": "Username or email already exists."}
    finally:
        cursor.close()
        db.close()
        
async def get_exercise_categories():
    db = get_Mysql_db()
    cursor = None
    
    try:
        cursor = db.cursor()  
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
        cursor = db.cursor()   
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
        cursor = db.cursor()  
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
        cursor = db.cursor()   
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
        cursor = db.cursor()  
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
        cursor = db.cursor()
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
        cursor = db.cursor()  # Remove dictionary=True
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
        cursor = db.cursor()  
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