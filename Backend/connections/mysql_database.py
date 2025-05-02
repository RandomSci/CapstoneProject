import mysql.connector
from connections.functions import *
import os

def get_Mysql_db(max_retries=5, retry_delay=2):
    host = os.getenv("MYSQL_HOST", "db")  
    user = os.getenv("MYSQL_USER", "root")
    password = os.getenv("MYSQL_PASSWORD", "root")  
    database = os.getenv("MYSQL_DB", "perceptronx")
    
    for attempt in range(max_retries):
        try:
            connection = mysql.connector.connect(
                host=host,
                user=user,
                password=password,
                database=database,
                auth_plugin='mysql_native_password'
            )
            return connection
        except mysql.connector.Error as err:
            if attempt < max_retries - 1:
                print(f"Database connection attempt {attempt+1} failed: {err}. Retrying in {retry_delay} seconds...")
                time.sleep(retry_delay)
            else:
                print(f"Failed to connect to database after {max_retries} attempts: {err}")
                raise

def Register_User_Web(first_name, last_name, company_email, password):
    db = get_Mysql_db()
    cursor = db.cursor()
    hashed_password = bcrypt.hashpw(password.password.encode("utf-8"), bcrypt.gensalt())
    try:
        cursor.execute("SELECT COUNT(*) FROM Therapists WHERE first_name = %s AND last_name = %s", (first_name, last_name))
        if cursor.fetchone()[0] > 0:
            raise HTTPException(status_code=400, detail="Username or email already exists.")
        cursor.execute(
            "INSERT INTO Therapists (first_name, last_name, company_email, password) VALUES (%s, %s, %s, %s)",
            (first_name, last_name, company_email, hashed_password.decode("utf-8"))
        )
        db.commit()
        return {"message": "User registered successfully"}
    except mysql.connector.IntegrityError:
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
            
async def main(user_id):
    patient_profile = await user_patient_profile(user_id)
    if not patient_profile:
        print("No patient profile found.")
        return
    
    therapist_id = patient_profile[0]['therapist_id']
    patient_id = patient_profile[0]['patient_id']
    
    therapist_data = await get_therapist_data(therapist_id)
    appointments = await get_appointment_data(patient_id)
    treatment_plans = await get_treatment_plans(patient_id)
    
    print(f"patient_profile: {patient_profile}")
    print(f"therapist_data: {therapist_data}")
    print(f"appointments: {appointments}")
    print(f"treatment_plans: {treatment_plans}")
    
    for plan in treatment_plans:
        plan_exercises = await get_treatment_plan_exercises(plan['plan_id'])
        print(f"Plan {plan['name']} exercises: {plan_exercises}")
        
        for exercise in plan_exercises:
            exercise_details = await get_exercise_details(exercise['exercise_id'])
            print(f"Exercise {exercise['exercise_id']} details: {exercise_details}")

#await main(48)