from connections.functions import *
from connections.mysql_database import *
from connections.redis_database import *
from connections.mongo_db import *
from contextlib import asynccontextmanager
import traceback
import os


def find_best_matching_image(therapist_id, requested_filename, static_dir):
    user_images_dir = os.path.join(static_dir, "assets/images/user")
    
    if requested_filename and os.path.exists(os.path.join(user_images_dir, requested_filename)):
        return requested_filename
    
    if therapist_id:
        prefix = f"therapist_{therapist_id}_"
        
        if os.path.exists(user_images_dir) and os.path.isdir(user_images_dir):
            try:
                all_files = os.listdir(user_images_dir)
                
                matching_files = [f for f in all_files if f.startswith(prefix)]
                
                if matching_files:
                    print(f"Found matching image for therapist {therapist_id}: {matching_files[0]}")
                    return matching_files[0]
            except Exception as e:
                print(f"Error searching for matching images: {e}")
    
    avatar_id = (therapist_id % 10) if therapist_id else 1
    default_image = f"avatar-{avatar_id}.jpg"
    
    if os.path.exists(os.path.join(user_images_dir, default_image)):
        return default_image
    else:
        return "avatar-1.jpg"

def getIP():
    try:
        hostname = socket.gethostname()
        
        ip = socket.gethostbyname(hostname)
        
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect(('10.255.255.255', 1))
            ip = s.getsockname()[0]
        except Exception:
            ip = '127.0.0.1'
        finally:
            s.close()
        
        full_ip = f"http://{ip}:8000"
        print(f"Using IP: {ip}")
        print(f"Base URL: {full_ip}")
        return full_ip
    except Exception as e:
        print(f"Error detecting IP: {e}")
        print("Defaulting to localhost")
        return "http://127.0.0.1:8000"

def safely_parse_json_field(field_value, default=None):
    """
    Safely parse a JSON field from the database.
    Returns the parsed JSON or the default value if parsing fails.
    """
    if field_value is None:
        return default if default is not None else []
    
    if isinstance(field_value, (list, dict)):
        return field_value
        
    if isinstance(field_value, bytes):
        field_value = field_value.decode('utf-8')
        
    if not isinstance(field_value, str):
        return default if default is not None else []
        
    try:
        return json.loads(field_value)
    except (json.JSONDecodeError, TypeError):
        if isinstance(field_value, str) and ',' in field_value:
            return [item.strip() for item in field_value.split(',')]
        return default if default is not None else []

def ensure_bytes(data):
    """
    Ensure data is in bytes format, converting from string if necessary.
    Use this before writing data to binary mode files or sending to functions expecting bytes.
    """
    if data is None:
        return b''
    if isinstance(data, bytes):
        return data
    if isinstance(data, str):
        return data.encode('utf-8')
    return str(data).encode('utf-8')

def ensure_str(data):
    """
    Ensure data is in string format, converting from bytes if necessary.
    Use this before inserting data into database fields expecting strings.
    """
    if data is None:
        return None
    if isinstance(data, str):
        return data
    if isinstance(data, bytes):
        return data.decode('utf-8')
    return str(data)

async def test_redis_connection():
    pass

@asynccontextmanager
async def lifespan(app: FastAPI):
    if not hasattr(app.state, 'base_url') or not app.state.base_url:
        app.state.base_url = getIP()

    await test_redis_connection()
    yield

def configure_static_files(app):
    static_dir = os.environ.get("STATIC_DIR", None)
    
    if not static_dir:
        current_file = FilePath(__file__).resolve()
        project_root = current_file.parent.parent.parent
        static_dir = str(project_root / "Frontend_Web" / "static")
    
    if not os.path.exists(static_dir):
        os.makedirs(static_dir, exist_ok=True)
        print(f"Created static directory: {static_dir}")
    
    if not hasattr(app.state, 'base_url') or not app.state.base_url:
        base_url = os.environ.get("BASE_URL", None)
        if base_url:
            app.state.base_url = base_url
        else:
            app.state.base_url = getIP()
    
    if not any(route.path == "/static" for route in app.routes):
        app.mount("/static", StaticFiles(directory=static_dir), name="static")
        print(f"Static directory mounted: {static_dir}")
    
    print(f"Base URL configured as: {app.state.base_url}")
    
    templates_dir = os.path.join(os.path.dirname(static_dir), "templates")
    if os.path.exists(templates_dir) and not any(route.path == "/dist" for route in app.routes):
        app.mount("/dist", StaticFiles(directory=templates_dir), name="templates")
        print(f"Templates directory mounted: {templates_dir}")
    
    return Jinja2Templates(directory=templates_dir) if os.path.exists(templates_dir) else None

app = FastAPI(title="PerceptronX API", version="1.0", lifespan=lifespan)

templates = configure_static_files(app)

class PlatformRoutingMiddleware(BaseHTTPMiddleware):
    def __init__(self, app):
        super().__init__(app)
        self.web_redirects = {
            "/": "/Therapist_Login",

        }

    async def dispatch(self, request: Request, call_next):
        user_agent = request.headers.get("User-Agent")
        if not user_agent:
            return await call_next(request)

        ua = user_agents.parse(user_agent)

        if ua.is_mobile:
            return await call_next(request)
        
        current_path = request.url.path
        if current_path in self.web_redirects:
            return RedirectResponse(url=self.web_redirects[current_path])

        return await call_next(request)

app.add_middleware(PlatformRoutingMiddleware)

@app.on_event("startup")
async def startup_event():
    print("Testing Redis connection...")
    try:
        await r.set("startup_test_key", "test_value")
        test_value = await r.get("startup_test_key")
        print(f"Redis connection test result: {test_value}")
        
        await r.hset("startup_test_hash", mapping={"test_field": "test_value"})
        hash_value = await r.hgetall("startup_test_hash")
        print(f"Redis hash operation test result: {hash_value}")
        
        await r.delete("startup_test_key", "startup_test_hash")
        
        print("Redis connection and operations successfully tested")
    except Exception as e:
        print(f"ERROR: Redis connection failed: {e}")
        print("APPLICATION WARNING: Session management will not work correctly!")


router = APIRouter()
app.include_router(router)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], 
    allow_credentials=True,
    allow_methods=["*"], 
    allow_headers=["*"],  
)

current_file = FilePath(__file__).resolve()
project_root = current_file.parent.parent.parent

static_directory = project_root / "Frontend_Web" / "static"
templates_directory = project_root / "Frontend_Web" / "templates"

templates = Jinja2Templates(directory=templates_directory)

print(f"Static directory: {static_directory}")
print(f"Templates directory: {templates_directory}")

if static_directory.exists():
    app.mount("/static", StaticFiles(directory=str(static_directory)), name="static")
    print(f"Static directory mounted successfully")
else:
    print(f"Warning: Static directory does not exist: {static_directory}")

if templates_directory.exists():
    app.mount("/dist", StaticFiles(directory=str(templates_directory)), name="templates")
    print(f"Templates directory mounted successfully")
else:
    print(f"Warning: Templates directory does not exist: {templates_directory}")

app.include_router(router)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], 
    allow_credentials=True,
    allow_methods=["*"], 
    allow_headers=["*"],  
)

def Routes():
    @app.get("/")
    async def Home(request: Request):
        session_id = request.cookies.get("session_id")

        if session_id:
            try:
                user_data = await get_redis_session(session_id)
                if user_data:
                    return {"status": "valid", "user": user_data}
            except Exception as e:
                print(f"Session validation error: {e}")

        return {"status": "valid"}
    
    @app.get("/front-page")
    async def front_page(request: Request):
        session_id = request.cookies.get("session_id")
        print(f"Session ID from cookie: {session_id}")
        if not session_id:
            print("No session ID found in cookie")
            return RedirectResponse(url="/Therapist_Login")
        try:
            session_data = await get_redis_session(session_id)
            print(f"Session data retrieved: {session_data}")
            if not session_data:
                print("Session data is None")
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)
            try:
 
                cursor.execute(
                    "SELECT first_name, last_name, profile_image FROM Therapists WHERE id = %s", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                print(f"Therapist data: {therapist}")
                if not therapist:
                    print(f"No therapist found for ID: {session_data['user_id']}")
                    return RedirectResponse(url="/Therapist_Login")

 
                cursor.execute(
                    """SELECT m.message_id, m.subject, m.content, m.created_at,
                            CASE 
                                WHEN m.sender_type = 'therapist' THEN t.first_name
                                WHEN m.sender_type = 'user' THEN u.username
                                ELSE 'Unknown'
                            END as first_name,
                            CASE
                                WHEN m.sender_type = 'therapist' THEN t.last_name
                                ELSE ''
                            END as last_name,
                            CASE
                                WHEN m.sender_type = 'therapist' THEN COALESCE(t.profile_image, 'avatar-1.jpg')
                                WHEN m.sender_type = 'user' THEN 'avatar-2.jpg'
                                ELSE 'avatar-2.jpg'
                            END as profile_image
                        FROM Messages m
                        LEFT JOIN Therapists t ON m.sender_id = t.id AND m.sender_type = 'therapist'
                        LEFT JOIN users u ON m.sender_id = u.user_id AND m.sender_type = 'user'
                        WHERE m.recipient_id = %s AND m.is_read = FALSE
                        ORDER BY m.created_at DESC
                        LIMIT 4""",
                    (session_data["user_id"],)
                )
                
                messages_result = cursor.fetchall()

 
                recent_messages = []
                for message in messages_result:
                    message_with_time = message.copy()

 
                    timestamp = message['created_at']
                    now = datetime.datetime.now()
                    if isinstance(timestamp, datetime.datetime):
                        diff = now - timestamp
                        if timestamp.date() == now.date():
                            message_with_time['time_display'] = timestamp.strftime('%I:%M %p')

                            minutes_ago = diff.seconds // 60
                            if minutes_ago < 60:
                                message_with_time['time_ago'] = f"{minutes_ago} min ago"
                            else:
                                hours_ago = minutes_ago // 60
                                message_with_time['time_ago'] = f"{hours_ago}-{hours_ago}"


                        elif timestamp.date() == (now - timedelta(days=1)).date():
                            message_with_time['time_display'] = "Yesterday"
                            message_with_time['time_ago'] = timestamp.strftime('%I:%M %p')
                        else:
                            message_with_time['time_display'] = timestamp.strftime('%d %b')
                            message_with_time['time_ago'] = timestamp.strftime('%Y')

                    recent_messages.append(message_with_time)

 
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0

 
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Appointments WHERE therapist_id = %s", 
                    (session_data["user_id"],)
                )
                appointments_result = cursor.fetchone()
                appointments_count = appointments_result['count'] if appointments_result else 0

 
                last_month = datetime.datetime.now() - timedelta(days=30)
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Appointments WHERE therapist_id = %s AND created_at < %s", 
                    (session_data["user_id"], last_month)
                )
                last_month_appointments = cursor.fetchone()
                last_month_count = last_month_appointments['count'] if last_month_appointments else 0

 
                appointments_monthly_diff = appointments_count - last_month_count
                appointments_growth = round((appointments_monthly_diff / max(last_month_count, 1)) * 100, 1)

 
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Patients WHERE therapist_id = %s AND status = 'Active'", 
                    (session_data["user_id"],)
                )
                active_patients_result = cursor.fetchone()
                active_patients_count = active_patients_result['count'] if active_patients_result else 0

 
                this_month_start = datetime.datetime.now().replace(day=1, hour=0, minute=0, second=0, microsecond=0)
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Patients WHERE therapist_id = %s AND created_at >= %s", 
                    (session_data["user_id"], this_month_start)
                )
                new_patients_result = cursor.fetchone()
                new_patients_monthly = new_patients_result['count'] if new_patients_result else 0

 
                last_month_start = (this_month_start - timedelta(days=1)).replace(day=1)
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Patients WHERE therapist_id = %s AND created_at BETWEEN %s AND %s", 
                    (session_data["user_id"], last_month_start, this_month_start)
                )
                last_month_new_patients = cursor.fetchone()
                last_month_new_count = last_month_new_patients['count'] if last_month_new_patients else 1
                patient_growth = round((new_patients_monthly / max(last_month_new_count, 1)) * 100, 1)

 
                cursor.execute(
                    "SELECT COUNT(*) as count FROM TreatmentPlans WHERE therapist_id = %s", 
                    (session_data["user_id"],)
                )
                treatment_plans_result = cursor.fetchone()
                treatment_plans_count = treatment_plans_result['count'] if treatment_plans_result else 0

 
                cursor.execute(
                    "SELECT COUNT(*) as count FROM TreatmentPlans WHERE therapist_id = %s AND created_at >= %s", 
                    (session_data["user_id"], this_month_start)
                )
                new_plans_result = cursor.fetchone()
                new_plans_monthly = new_plans_result['count'] if new_plans_result else 0

 
                cursor.execute(
                    "SELECT COUNT(*) as count FROM TreatmentPlans WHERE therapist_id = %s AND created_at BETWEEN %s AND %s", 
                    (session_data["user_id"], last_month_start, this_month_start)
                )
                last_month_plans = cursor.fetchone()
                last_month_plans_count = last_month_plans['count'] if last_month_plans else 1
                plans_growth = round((new_plans_monthly / max(last_month_plans_count, 1)) * 100, 1)

 
                cursor.execute(
                    "SELECT AVG(adherence_rate) as avg_rate FROM PatientMetrics WHERE therapist_id = %s", 
                    (session_data["user_id"],)
                )
                adherence_result = cursor.fetchone()
                average_adherence_rate = round(adherence_result['avg_rate'], 1) if adherence_result and adherence_result['avg_rate'] is not None else 0

 
                cursor.execute(
                    """SELECT AVG(adherence_rate) as avg_rate 
                    FROM PatientMetrics 
                    WHERE therapist_id = %s AND measurement_date BETWEEN %s AND %s""", 
                    (session_data["user_id"], last_month_start, this_month_start)
                )
                last_month_adherence = cursor.fetchone()
                last_month_adherence_rate = last_month_adherence['avg_rate'] if last_month_adherence and last_month_adherence['avg_rate'] is not None else 0

                adherence_monthly_diff = round(average_adherence_rate - last_month_adherence_rate, 1)
                adherence_change = abs(adherence_monthly_diff)

                if adherence_monthly_diff >= 0:
                    adherence_trend_direction = "up"
                    adherence_trend_color = "success"
                    adherence_direction = "Up by"
                else:
                    adherence_trend_direction = "down"
                    adherence_trend_color = "warning"
                    adherence_direction = "Down"

                week_ago = datetime.datetime.now() - timedelta(days=7)
                cursor.execute(
                    """SELECT AVG(
                        CASE 
                            WHEN repetitions IS NOT NULL THEN (repetitions_completed / repetitions) * 100
                            ELSE (sets_completed / sets) * 100
                        END
                    ) as completion_rate
                    FROM PatientExerciseProgress pep
                    JOIN TreatmentPlanExercises tpe ON pep.plan_exercise_id = tpe.plan_exercise_id
                    WHERE pep.completion_date >= %s""", 
                    (week_ago,)
                )
                completion_result = cursor.fetchone()
                weekly_completion_rate = round(completion_result['completion_rate'], 0) if completion_result and completion_result['completion_rate'] is not None else 0

 
                cursor.execute(
                    """SELECT p.patient_id, p.first_name, p.last_name, p.diagnosis, p.status,
                        COALESCE(AVG(pm.adherence_rate), 0) as adherence_rate
                    FROM Patients p
                    LEFT JOIN PatientMetrics pm ON p.patient_id = pm.patient_id
                    WHERE p.therapist_id = %s
                    GROUP BY p.patient_id
                    ORDER BY p.created_at DESC
                    LIMIT 5""", 
                    (session_data["user_id"],)
                )
                recent_patients_result = cursor.fetchall()

 
                recent_patients = []
                for patient in recent_patients_result:
                    status_color = "success"
                    if patient['status'] == "Inactive":
                        status_color = "danger"
                    elif patient['status'] == "At Risk":
                        status_color = "warning"

                    patient_with_color = patient.copy()
                    patient_with_color['status_color'] = status_color
                    patient_with_color['adherence_rate'] = round(patient['adherence_rate'], 0)
                    recent_patients.append(patient_with_color)

 
                cursor.execute(
                    "SELECT AVG(recovery_progress) as avg_recovery FROM PatientMetrics WHERE therapist_id = %s", 
                    (session_data["user_id"],)
                )
                recovery_result = cursor.fetchone()
                avg_recovery_rate = round(recovery_result['avg_recovery'], 1) if recovery_result and recovery_result['avg_recovery'] is not None else 0

 
                cursor.execute(
                    """SELECT AVG(
                        CASE 
                            WHEN repetitions IS NOT NULL THEN (repetitions_completed / repetitions) * 100
                            ELSE (sets_completed / sets) * 100
                        END
                    ) as completion_rate
                    FROM PatientExerciseProgress pep
                    JOIN TreatmentPlanExercises tpe ON pep.plan_exercise_id = tpe.plan_exercise_id"""
                )
                overall_completion = cursor.fetchone()
                exercise_completion_rate = round(overall_completion['completion_rate'], 1) if overall_completion and overall_completion['completion_rate'] is not None else 0

 
                cursor.execute(
                    "SELECT AVG(rating) as avg_rating FROM feedback"
                )
                satisfaction_result = cursor.fetchone()
                avg_satisfaction = satisfaction_result['avg_rating'] if satisfaction_result and satisfaction_result['avg_rating'] is not None else 0

                if avg_satisfaction >= 4:
                    patient_satisfaction = "High"
                elif avg_satisfaction >= 3:
                    patient_satisfaction = "Medium"
                else:
                    patient_satisfaction = "Low"

 
                cursor.execute(
                    "SELECT AVG(functionality_score) as avg_score FROM PatientMetrics WHERE therapist_id = %s", 
                    (session_data["user_id"],)
                )
                progress_result = cursor.fetchone()
                progress_metric_value = progress_result['avg_score'] if progress_result and progress_result['avg_score'] is not None else 0

 
                cursor.execute(
                    """(SELECT 'video' as type, 'New Exercise Uploaded' as title, e.name as primary_detail, 
                        CONCAT(e.duration, ' min') as secondary_detail, e.created_at as timestamp,
                        CONCAT('/exercises/', e.exercise_id) as link
                    FROM Exercises e
                    WHERE e.therapist_id = %s
                    ORDER BY e.created_at DESC
                    LIMIT 3)
                    UNION
                    (SELECT 'user-plus' as type, 'New Patient Added' as title, 
                        CONCAT(p.first_name, ' ', p.last_name) as primary_detail, 
                        p.diagnosis as secondary_detail, p.created_at as timestamp,
                        CONCAT('/patients/', p.patient_id) as link
                    FROM Patients p
                    WHERE p.therapist_id = %s
                    ORDER BY p.created_at DESC
                    LIMIT 3)
                    UNION
                    (SELECT 'report-medical' as type, 'Progress Report Updated' as title, 
                        CONCAT(p.first_name, ' ', p.last_name) as primary_detail, 
                        CONCAT('+', pm.recovery_progress, '% improvement') as secondary_detail, 
                        pm.created_at as timestamp,
                        CONCAT('/patients/', p.patient_id) as link
                    FROM PatientMetrics pm
                    JOIN Patients p ON pm.patient_id = p.patient_id
                    WHERE pm.therapist_id = %s
                    ORDER BY pm.created_at DESC
                    LIMIT 3)
                    ORDER BY timestamp DESC
                    LIMIT 3""", 
                    (session_data["user_id"], session_data["user_id"], session_data["user_id"])
                )
                activities_result = cursor.fetchall()

 
                recent_activities = []
                for activity in activities_result:
                    activity_with_color = activity.copy()

 
                    if activity['type'] == 'video':
                        activity_with_color['color'] = 'success'
                        activity_with_color['icon'] = 'video'
                    elif activity['type'] == 'user-plus':
                        activity_with_color['color'] = 'primary'
                        activity_with_color['icon'] = 'user-plus'
                    else:
                        activity_with_color['color'] = 'warning'
                        activity_with_color['icon'] = 'report-medical'

 
                    timestamp = activity['timestamp']
                    now = datetime.datetime.now()
                    if isinstance(timestamp, datetime.datetime):
                        if timestamp.date() == now.date():
                            activity_with_color['timestamp'] = f"Today, {timestamp.strftime('%I:%M %p')}"
                        elif timestamp.date() == (now - timedelta(days=1)).date():
                            activity_with_color['timestamp'] = f"Yesterday, {timestamp.strftime('%I:%M %p')}"
                        else:
                            activity_with_color['timestamp'] = f"{(now - timestamp).days} days ago"

                    recent_activities.append(activity_with_color)

 
                cursor.execute(
                    """SELECT 
                        DATE_FORMAT(completion_date, '%a') as day, 
                        COUNT(*) as count
                    FROM PatientExerciseProgress
                    WHERE completion_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                    GROUP BY DATE_FORMAT(completion_date, '%a')
                    ORDER BY FIELD(day, 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun')"""
                )
                weekly_activity = cursor.fetchall()

 
                days_of_week = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
                activity_data = {day: 0 for day in days_of_week}

                for record in weekly_activity:
                    if record['day'] in activity_data:
                        activity_data[record['day']] = record['count']

                chart_data = [{'day': day, 'count': count} for day, count in activity_data.items()]

                cursor.execute(
                    """SELECT 
                        DATE_FORMAT(completion_date, '%d') as date, 
                        COUNT(*) as count
                    FROM PatientExerciseProgress
                    WHERE completion_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                    GROUP BY DATE_FORMAT(completion_date, '%d')
                    ORDER BY date"""
                )
                monthly_activity = cursor.fetchall()
                monthly_chart_data = [{'date': record['date'], 'count': record['count']} for record in monthly_activity]

                cursor.execute(
                    """SELECT 
                        DATE_FORMAT(measurement_date, '%d %b') as date,
                        AVG(functionality_score) as score 
                    FROM PatientMetrics 
                    WHERE measurement_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                    AND therapist_id = %s
                    GROUP BY DATE_FORMAT(measurement_date, '%d %b')
                    ORDER BY measurement_date""", 
                    (session_data["user_id"],)
                )
                progress_chart_data = cursor.fetchall()
                progress_data = [{'date': record['date'], 'score': float(record['score']) if record['score'] is not None else 0} for record in progress_chart_data]

 
                cursor.execute(
                    """SELECT 
                        CASE 
                            WHEN (
                                CASE 
                                    WHEN repetitions IS NOT NULL THEN (repetitions_completed / repetitions) * 100
                                    ELSE (sets_completed / sets) * 100
                                END
                            ) >= 90 THEN 'Completed'
                            WHEN (
                                CASE 
                                    WHEN repetitions IS NOT NULL THEN (repetitions_completed / repetitions) * 100
                                    ELSE (sets_completed / sets) * 100
                                END
                            ) >= 50 THEN 'Partial'
                            ELSE 'Missed'
                        END as status,
                        COUNT(*) as count
                    FROM PatientExerciseProgress pep
                    JOIN TreatmentPlanExercises tpe ON pep.plan_exercise_id = tpe.plan_exercise_id
                    WHERE completion_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                    GROUP BY status"""
                )
                completion_breakdown = cursor.fetchall()
                donut_data = {'Completed': 0, 'Partial': 0, 'Missed': 0}
                for record in completion_breakdown:
                    if record['status'] in donut_data:
                        donut_data[record['status']] = record['count']

 
                print("Rendering dashboard template with dynamic data")
                return templates.TemplateResponse(
                    "dist/dashboard/index.html", 
                    {
                        "request": request,
                        "therapist": therapist or None,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "appointments_count": appointments_count,
                        "appointments_growth": appointments_growth,
                        "appointments_monthly_diff": appointments_monthly_diff,
                        "active_patients_count": active_patients_count,
                        "patient_growth": patient_growth,
                        "new_patients_monthly": new_patients_monthly,
                        "treatment_plans_count": treatment_plans_count,
                        "plans_growth": plans_growth,
                        "new_plans_monthly": new_plans_monthly,
                        "average_adherence_rate": average_adherence_rate,
                        "adherence_trend_color": adherence_trend_color,
                        "adherence_trend_direction": adherence_trend_direction,
                        "adherence_change": adherence_change,
                        "adherence_direction": adherence_direction,
                        "adherence_monthly_diff": adherence_monthly_diff,
                        "weekly_completion_rate": weekly_completion_rate,
                        "recent_patients": recent_patients,
                        "avg_recovery_rate": avg_recovery_rate,
                        "exercise_completion_rate": exercise_completion_rate,
                        "patient_satisfaction": patient_satisfaction,
                        "progress_metric_value": progress_metric_value,
                        "recent_activities": recent_activities,
                        "chart_data": chart_data,
                        "monthly_chart_data": monthly_chart_data,
                        "progress_data": progress_data,
                        "donut_data": donut_data,
                        "recent_messages": recent_messages,
                        "unread_messages_count": unread_messages_count
                    }
                )
            except Exception as e:
                print(f"Database error in front-page route: {e}")
                return RedirectResponse(url="/Therapist_Login")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in front-page route: {e}")
            return RedirectResponse(url="/Therapist_Login")

    async def get_therapist_data(db, user_id):
        """Retrieve therapist data from database"""
        try:
            cursor = db.cursor(dictionary=True)
            cursor.execute(
                """SELECT id, first_name, last_name, company_email, profile_image, 
                        bio, experience_years, specialties, education, languages, 
                        address, rating, review_count, 
                        is_accepting_new_patients, average_session_length
                FROM Therapists 
                WHERE id = %s""", 
                (user_id,)
            )
            therapist = cursor.fetchone()
            
 
            for field in ['specialties', 'education', 'languages']:
                if therapist[field] and isinstance(therapist[field], str):
                    try:
                        therapist[field] = json.loads(therapist[field])
                    except:
                        therapist[field] = []
                elif therapist[field] is None:
                    therapist[field] = []
                    
            return therapist
        except Exception as e:
            print(f"Error retrieving therapist data: {e}")
            return {
                "first_name": "",
                "last_name": "",
                "company_email": "",
                "profile_image": "avatar-1.jpg",
                "specialties": [],
                "education": [],
                "languages": []
            }
        finally:
            if cursor:
                cursor.close()

    async def get_unread_messages_count(db, user_id):
        """Get count of unread messages"""
        try:
            cursor = db.cursor(dictionary=True)
            cursor.execute(
                "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                (user_id,)
            )
            result = cursor.fetchone()
            return result['count'] if result else 0
        except Exception as e:
            print(f"Error counting unread messages: {e}")
            return 0
        finally:
            if cursor:
                cursor.close()

    def get_all_specialties():
        """Return list of all specialties"""
        return [
            "Orthopedic Physical Therapy",
            "Neurological Physical Therapy",
            "Cardiovascular & Pulmonary Physical Therapy",
            "Pediatric Physical Therapy",
            "Geriatric Physical Therapy",
            "Sports Physical Therapy",
            "Women's Health Physical Therapy",
            "Manual Therapy",
            "Vestibular Rehabilitation",
            "Post-Surgical Rehabilitation",
            "Pain Management"
        ]
        
    @app.get("/api/therapist/{therapist_id}")
    async def get_therapist_api(therapist_id: int):
            """API endpoint to get therapist information"""
            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:
                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image, 
                            bio, experience_years, specialties, education, languages, 
                            address, latitude, longitude, rating, review_count, 
                            is_accepting_new_patients, average_session_length
                    FROM Therapists 
                    WHERE id = %s""", 
                    (therapist_id,)
                )
                therapist = cursor.fetchone()

                if not therapist:
                    return JSONResponse(
                        status_code=404,
                        content={"error": "Therapist not found"}
                    )

 
                for field in ['specialties', 'education', 'languages']:
                    if therapist[field] and isinstance(therapist[field], str):
                        try:
                            therapist[field] = json.loads(therapist[field])
                        except:
                            therapist[field] = []
                    elif therapist[field] is None:
                        therapist[field] = []

 
                response_data = {
                    "id": therapist["id"],
                    "name": f"{therapist['first_name']} {therapist['last_name']}",
                    "photoUrl": f"/static/assets/images/user/{therapist['profile_image']}",
                    "specialties": therapist["specialties"],
                    "bio": therapist["bio"] or "",
                    "experienceYears": therapist["experience_years"] or 0,
                    "education": therapist["education"],
                    "languages": therapist["languages"],
                    "address": therapist["address"] or "",
                    "latitude": therapist["latitude"] or 0,
                    "longitude": therapist["longitude"] or 0,
                    "rating": therapist["rating"] or 0,
                    "reviewCount": therapist["review_count"] or 0,
                    "isAcceptingNewPatients": bool(therapist["is_accepting_new_patients"]),
                    "averageSessionLength": therapist["average_session_length"] or 60
                }

                return response_data

            except Exception as e:
                print(f"Database error in get therapist API: {e}")
                return JSONResponse(
                    status_code=500,
                    content={"error": f"Internal server error: {str(e)}"}
                )
            finally:
                cursor.close()
                db.close()

    @app.get("/dashboard")
    async def dashboard(user = Depends(get_current_user)):
        return {"message": f"Welcome, {user['username']}!", "user_id": user["user_id"]}

    @app.post("/registerUser")
    async def registerUser(result: Register): 
        db = get_Mysql_db()
        cursor = db.cursor()
    
        hashed_password = bcrypt.hashpw(result.password.encode("utf-8"), bcrypt.gensalt())

        try:
            cursor.execute(
                "INSERT INTO users (username, email, password_hash) VALUES (%s, %s, %s)",
                (result.username, result.email, hashed_password.decode("utf-8"))
            )
            db.commit()
            return RedirectResponse(url="/", status_code=303)
        except mysql.connector.IntegrityError:
            return {"error": "Username or email already exists."}
        finally:
            cursor.close()
            db.close()
            
    @app.route("/Register_User_Web", methods=["GET", "POST"])
    async def Register_User_Web(request: Request):
        form = await request.form()
        first_name = form.get("first_name")
        last_name = form.get("last_name")
        company_email = form.get("company_email")
        password = form.get("password")

        if not all([first_name, last_name, company_email, password]):
            return templates.TemplateResponse("dist/pages/register.html", {
                "request": request,
                "error": "All fields are required."
            })

        db = get_Mysql_db()
        cursor = db.cursor()

        hashed_password = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt())

        try:
            cursor.execute(
                "INSERT INTO Therapists (first_name, last_name, company_email, password) VALUES (%s, %s, %s, %s)",
                (first_name, last_name, company_email, hashed_password.decode("utf-8"))
            )
            db.commit()
            return RedirectResponse(url="/", status_code=303)
        except mysql.connector.IntegrityError:
            return templates.TemplateResponse("dist/pages/register.html", {
                "request": request,
                "error": "Therapist with this email already exists."
            })
        finally:
            cursor.close()
            db.close()

    active_sessions: Dict[str, SessionData] = {}

    @app.post("/loginUser")
    async def loginUser(result: Login, response: Response):
        db = get_Mysql_db()
        cursor = db.cursor()

        try:
            cursor.execute(
                "SELECT user_id, password_hash FROM users WHERE username = %s",
                (result.username,)
            )
            user = cursor.fetchone()

            if user is None:
                raise HTTPException(status_code=401, detail="Invalid username or password")

            user_id, stored_password_hash = user[0], user[1].encode("utf-8")

            if bcrypt.checkpw(result.password.encode("utf-8"), stored_password_hash):
                session_id = await create_session(
                    user_id=user_id, 
                    email=result.username,
                )

                response.set_cookie(
                    key="session_id", 
                    value=session_id, 
                    httponly=True,
                    samesite="lax",
                    path="/"
                )
                print("response 152", response)

                return {"status": "valid"}
            else:
                raise HTTPException(status_code=401, detail="Invalid username or password")
        finally:
            cursor.close()
            db.close()
            
    @app.get("/getUserInfo") 
    async def get_user_info(request: Request):
        session_id = request.cookies.get("session_id")
        print(f"session_id: {session_id}")

        if not session_id:
            raise HTTPException(status_code=401, detail="Session not found") 

        session_data = await get_session_data(session_id)
        print(f"session_data: {session_data}")

        if not session_data:
            raise HTTPException(status_code=401, detail="Invalid session")

        user_id = session_data.user_id
        print(f"user_id: {user_id}")

        db = get_Mysql_db()
        cursor = db.cursor()

        try:
            cursor.execute("SELECT username, email, created_at FROM users WHERE user_id = %s", (user_id,))
            row = cursor.fetchone()

            if not row:
                raise HTTPException(status_code=404, detail="User not found")

            username, email, created_at = row
            return {
                "username": username,
                "email": email,
                "joined": str(created_at)
            }

        finally:
            cursor.close()
            db.close()


    @app.post("/logout")
    async def logout(request: Request):
        session_id = request.cookies.get("session_id")

        if session_id:
            await delete_session(session_id)

        response = JSONResponse(content={"message": "Logged out"})
        response.delete_cookie("session_id")
        return response
    
    @app.get("/logout")
    async def logout_get(request: Request):
        session_id = request.cookies.get("session_id")
        if session_id:
            await delete_session(session_id)
        response = RedirectResponse(url="/Therapist_Login")
        response.delete_cookie("session_id")
        return response

    async def create_session(user_id: int, email: str, remember: bool = False) -> str:
        session_id = secrets.token_hex(16)

        if remember:
            expires = datetime.datetime.now() + datetime.timedelta(days=30)
        else:
            expires = datetime.datetime.now() + datetime.timedelta(hours=24)

        active_sessions[session_id] = SessionData(
            user_id=user_id,
            email=email,
            expires=expires
        )

        return session_id

    async def delete_session(session_id: str) -> None:
        if session_id in active_sessions:
            del active_sessions[session_id]

    async def get_session_data(session_id: str) -> Optional[SessionData]:
        session = active_sessions.get(session_id)

        if not session:
            return None

        if session.expires < datetime.datetime.now():
            await delete_session(session_id)
            return None

        return session

    @app.get("/Therapist_Login")
    async def therapist_login_page(request: Request):
        session_id = request.cookies.get("session_id")
        if session_id:
            session = await get_session_data(session_id)
            if session:
                return RedirectResponse(url="/front-page")

        return templates.TemplateResponse("dist/pages/login.html", {"request": request})


    @app.post("/Therapist_Login")
    async def therapist_login(
        request: Request,
        email: str = Form(...),
        password: str = Form(...),
        remember: bool = Form(False)
    ):
        db = get_Mysql_db()
        cursor = db.cursor(dictionary=True)

        try:
            cursor.execute(
                "SELECT id, company_email, password, first_name, last_name FROM Therapists WHERE company_email = %s",
                (email,)
            )
            therapist = cursor.fetchone()

            if not therapist:
                return templates.TemplateResponse(
                    "dist/pages/login.html",
                    {"request": request, "error": "Invalid email or password"}
                )

            stored_password = therapist["password"]

            if bcrypt.checkpw(password.encode('utf-8'), stored_password.encode('utf-8')):
                session_data = {
                    "user_id": str(therapist["id"]),
                    "email": therapist["company_email"]
                }

                session_id = await create_redis_session(
                    data=session_data,
                )

                print(f"Session created: {session_id}")
                print(f"User ID: {therapist['id']}")

                response = RedirectResponse(url="/front-page", status_code=303)

                response.set_cookie(
                    key="session_id",
                    value=session_id,
                    httponly=True,
                    samesite="lax"
                )

                print(f"Response created with cookie: {response.headers}")
                return response
            else:
                return templates.TemplateResponse(
                    "dist/pages/login.html",
                    {"request": request, "error": "Invalid email or password"}
                )

        except Exception as e:
            print(f"Login error: {e}")
            return templates.TemplateResponse(
                "dist/pages/login.html",
                {"request": request, "error": f"Server error: {str(e)}"}
            )

        finally:
            cursor.close()
            db.close()
            
    @app.get("/reports/patients")
    async def patient_reports(request: Request):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            
            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)
            try:
                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image
                    FROM Therapists
                    WHERE id = %s""",
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")
                

                cursor.execute(
                    """SELECT patient_id, first_name, last_name, diagnosis, status
                    FROM Patients
                    WHERE therapist_id = %s
                    ORDER BY last_name, first_name""",
                    (session_data["user_id"],)
                )
                patients = cursor.fetchall()
                

                cursor.execute(
                    """SELECT COUNT(*) as pending_count
                    FROM ExerciseVideoSubmissions evs
                    JOIN Patients p ON evs.patient_id = p.patient_id
                    WHERE p.therapist_id = %s AND evs.status = 'Pending'""",
                    (session_data["user_id"],)
                )
                pending_count_result = cursor.fetchone()
                pending_count = pending_count_result['pending_count'] if pending_count_result else 0
                

                cursor.execute(
                    """SELECT 
                        evs.submission_id, 
                        evs.submission_date,
                        evs.status,
                        p.first_name,
                        p.last_name,
                        e.name as exercise_name,
                        tp.name as plan_name
                    FROM ExerciseVideoSubmissions evs
                    JOIN Patients p ON evs.patient_id = p.patient_id
                    JOIN Exercises e ON evs.exercise_id = e.exercise_id
                    JOIN TreatmentPlans tp ON evs.treatment_plan_id = tp.plan_id
                    WHERE p.therapist_id = %s
                    ORDER BY evs.submission_date DESC
                    LIMIT 3""",
                    (session_data["user_id"],)
                )
                submissions = cursor.fetchall()
                

                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                
                return templates.TemplateResponse(
                    "dist/reports/patient_reports.html",
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "patients": patients,
                        "submissions": submissions,
                        "pending_count": pending_count
                    }
                )
            except Exception as e:
                print(f"Database error in patient reports: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/front-page")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in patient reports: {e}")
            return RedirectResponse(url="/Therapist_Login")

    @app.get("/reports/patients/{patient_id}")
    async def patient_detailed_report(request: Request, patient_id: int):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:
                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image
                    FROM Therapists 
                    WHERE id = %s""", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()

                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")

                cursor.execute(
                    """SELECT * FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                patient = cursor.fetchone()

                if not patient:
                    return RedirectResponse(url="/reports/patients")

                cursor.execute(
                    """SELECT 
                        pep.*, 
                        tpe.sets, 
                        tpe.repetitions,
                        e.name as exercise_name, 
                        e.video_url as exercise_video_url, 
                        e.difficulty,
                        evs.submission_id as video_submission_id,
                        evs.video_url as submission_video_url
                        FROM PatientExerciseProgress pep
                        JOIN TreatmentPlanExercises tpe ON pep.plan_exercise_id = tpe.plan_exercise_id
                        JOIN Exercises e ON tpe.exercise_id = e.exercise_id
                        JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                        LEFT JOIN ExerciseVideoSubmissions evs ON 
                            evs.patient_id = tp.patient_id AND 
                            evs.exercise_id = e.exercise_id AND
                            DATE(evs.submission_date) = pep.completion_date
                        WHERE tp.patient_id = %s
                        ORDER BY pep.completion_date DESC, pep.created_at DESC""",
                    (patient_id,)
                )
                exercise_history = cursor.fetchall()


                for exercise in exercise_history:

                    if exercise.get('submission_video_url'):
                        filename = os.path.basename(exercise['submission_video_url'])
                        token = await generate_video_token(session_data["user_id"], filename)
                        exercise['tokenized_submission_url'] = f"/api/uploads/exercise_videos/{filename}?token={token}"
                    

                    if exercise.get('exercise_video_url'):
                        filename = os.path.basename(exercise['exercise_video_url'])
                        token = await generate_video_token(session_data["user_id"], filename)
                        exercise['tokenized_exercise_url'] = f"/api/uploads/exercise_videos/{filename}?token={token}"

                cursor.execute(
                    """SELECT * FROM TreatmentPlans
                    WHERE patient_id = %s
                    ORDER BY created_at DESC""",
                    (patient_id,)
                )
                treatment_plans = cursor.fetchall()

                cursor.execute(
                    """SELECT * FROM PatientMetrics
                    WHERE patient_id = %s
                    ORDER BY measurement_date DESC""",
                    (patient_id,)
                )
                patient_metrics = cursor.fetchall()
                
                for metric in patient_metrics:
                    if 'measurement_date' in metric and metric['measurement_date']:
                        metric['measurement_date'] = metric['measurement_date'].strftime('%Y-%m-%d')

                cursor.execute(
                    """SELECT f.* FROM feedback f
                    JOIN users u ON f.user_id = u.user_id
                    JOIN Patients p ON u.user_id = p.user_id
                    WHERE p.patient_id = %s
                    ORDER BY f.created_at DESC""",
                    (patient_id,)
                )
                patient_feedback = cursor.fetchall()

                cursor.execute(
                    """SELECT evs.*, e.name as exercise_name, tp.name as plan_name, evs.video_url
                    FROM ExerciseVideoSubmissions evs
                    JOIN Exercises e ON evs.exercise_id = e.exercise_id
                    JOIN TreatmentPlans tp ON evs.treatment_plan_id = tp.plan_id
                    WHERE evs.patient_id = %s
                    ORDER BY evs.submission_date DESC
                    LIMIT 6""",
                    (patient_id,)
                )
                video_submissions = cursor.fetchall()
                

                for submission in video_submissions:
                    if submission.get('video_url'):
                        filename = os.path.basename(submission['video_url'])
                        token = await generate_video_token(session_data["user_id"], filename)
                        submission['tokenized_video_url'] = f"/api/uploads/exercise_videos/{filename}?token={token}"

                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0


                print(f"Found {len(exercise_history)} exercise history records")
                print(f"Found {len(video_submissions)} video submissions")

                return templates.TemplateResponse(
                    "dist/reports/patient_detailed_report.html",
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "patient": patient,
                        "exercise_history": exercise_history,
                        "treatment_plans": treatment_plans,
                        "patient_metrics": patient_metrics,
                        "patient_feedback": patient_feedback,
                        "video_submissions": video_submissions
                    }
                )

            except Exception as e:
                print(f"Database error in patient detailed report: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/reports/patients")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in patient detailed report: {e}")
            return RedirectResponse(url="/Therapist_Login")

    @app.post("/exercises/add")
    async def add_exercise(
        request: Request,
        name: str = Form(...),
        category_id: Optional[int] = Form(None),
        description: Optional[str] = Form(None),
        video_source: Optional[str] = Form(None),
        video_url: Optional[str] = Form(None),
        difficulty: Optional[str] = Form(None),
        duration: Optional[int] = Form(None),
        instructions: Optional[str] = Form(None),
        video_upload: Optional[UploadFile] = File(None),
        user = Depends(get_current_user)
    ):
        """Route to handle adding a new exercise with large file upload support"""
        db = get_Mysql_db()
        cursor = None
        
        try:
            cursor = db.cursor()
            

            final_video_url = None
            video_type = 'none'
            video_size = None
            video_filename = None
            

            if video_source == 'youtube' and video_url:
                final_video_url = video_url
                video_type = 'youtube'
            

            elif video_source == 'upload' and video_upload and video_upload.filename:

                current_file = Path(__file__).resolve()
                project_root = current_file.parent.parent.parent
                uploads_dir = project_root / "Frontend_Web" / "static" / "assets" / "videos" / "exercises"
                uploads_dir.mkdir(parents=True, exist_ok=True)
                

                video_filename = video_upload.filename
                

                file_extension = video_filename.split(".")[-1].lower()
                unique_filename = f"exercise_{int(time.time())}_{secrets.token_hex(4)}.{file_extension}"
                file_path = uploads_dir / unique_filename
                

                video_content = await video_upload.read()
                video_size = len(video_content)
                

                async with aiofiles.open(file_path, "wb") as f:
                    await f.write(video_content)
                

                final_video_url = f"/static/assets/videos/exercises/{unique_filename}"
                video_type = 'upload'
            

            cursor.execute(
                """INSERT INTO Exercises 
                (name, category_id, description, video_url, video_type, video_size, video_filename, 
                difficulty, duration, instructions) 
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
                (name, category_id, description, final_video_url, video_type, video_size, 
                video_filename, difficulty, duration, instructions)
            )
            db.commit()
            
            return RedirectResponse(url="/exercises", status_code=303)
        except Exception as e:
            if db:
                db.rollback()
            print(f"Error adding exercise: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            
            categories = await get_exercise_categories()
            
            therapist_data = await get_therapist_data(user["user_id"])
            return templates.TemplateResponse(
                "dist/exercises/add_exercise.html", 
                {
                    "request": request,
                    "error": f"Error adding exercise: {str(e)}",
                    "categories": categories,
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"]
                },
                status_code=400
            )
        finally:
            if cursor:
                cursor.close()
            if db:
                db.close()
                
    @app.get("/exercises/submissions")
    async def view_exercise_submissions(request: Request):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            

            print(f"Session data: {session_data}")
            
            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)
            try:
                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image
                    FROM Therapists
                    WHERE id = %s""",
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                if not therapist:
                    print(f"Therapist not found for user_id: {session_data['user_id']}")
                    return RedirectResponse(url="/Therapist_Login")
                

                print(f"Found therapist: {therapist}")
                

                cursor.execute(
                    """SELECT COUNT(*) as patient_count 
                    FROM Patients 
                    WHERE therapist_id = %s""",
                    (therapist["id"],)
                )
                patient_count = cursor.fetchone()['patient_count']
                print(f"Therapist has {patient_count} assigned patients")
                

                cursor.execute(
                    """SELECT evs.*, p.first_name, p.last_name, e.name as exercise_name, tp.name as plan_name
                    FROM ExerciseVideoSubmissions evs
                    JOIN Patients p ON evs.patient_id = p.patient_id
                    JOIN Exercises e ON evs.exercise_id = e.exercise_id
                    JOIN TreatmentPlans tp ON evs.treatment_plan_id = tp.plan_id
                    WHERE p.therapist_id = %s
                    ORDER BY evs.submission_date DESC""",
                    (therapist["id"],)  
                )
                submissions = cursor.fetchall()
                

                print(f"Found {len(submissions)} submissions for therapist_id: {therapist['id']}")
                
                cursor.execute(
                    """SELECT COUNT(*) as pending_count
                    FROM ExerciseVideoSubmissions evs
                    JOIN Patients p ON evs.patient_id = p.patient_id
                    WHERE p.therapist_id = %s AND evs.status = 'Pending'""",
                    (session_data["user_id"],)
                )
                pending_count = cursor.fetchone()['pending_count']


                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0

                return templates.TemplateResponse(
                    "dist/exercises/submissions.html",
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "submissions": submissions,
                        "pending_count": pending_count
                    }
                )

            except Exception as e:
                print(f"Database error in exercise submissions: {e}")
                return RedirectResponse(url="/front-page")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in exercise submissions: {e}")
            return RedirectResponse(url="/Therapist_Login")

    @app.get("/exercises/submissions/{submission_id}")
    async def view_exercise_submission(request: Request, submission_id: int):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            
            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)
            try:
                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image
                    FROM Therapists
                    WHERE id = %s""",
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")
                
                cursor.execute(
                    """SELECT evs.*, p.first_name, p.last_name, p.patient_id,
                    e.name as exercise_name, tp.name as plan_name
                    FROM ExerciseVideoSubmissions evs
                    JOIN Patients p ON evs.patient_id = p.patient_id
                    JOIN Exercises e ON evs.exercise_id = e.exercise_id
                    JOIN TreatmentPlans tp ON evs.treatment_plan_id = tp.plan_id
                    WHERE evs.submission_id = %s AND p.therapist_id = %s""",
                    (submission_id, session_data["user_id"])
                )
                submission = cursor.fetchone()
                if not submission:
                    return RedirectResponse(url="/exercises/submissions")
                

                if submission and "video_url" in submission and submission["video_url"]:

                    filename = os.path.basename(submission["video_url"])
                    

                    token = await generate_video_token(session_data["user_id"], filename)
                    

                    query_params = urlencode({"token": token})
                    submission["video_url"] = f"/api/uploads/exercise_videos/{filename}?{query_params}"
                
                cursor.execute(
                    """SELECT evs.*, e.name as exercise_name
                    FROM ExerciseVideoSubmissions evs
                    JOIN Exercises e ON evs.exercise_id = e.exercise_id
                    WHERE evs.patient_id = %s
                    AND evs.exercise_id = %s
                    AND evs.submission_id != %s
                    ORDER BY evs.submission_date DESC
                    LIMIT 5""",
                    (submission['patient_id'], submission['exercise_id'], submission_id)
                )
                previous_submissions = cursor.fetchall()
                
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                
                return templates.TemplateResponse(
                    "dist/exercises/submission_detail.html",
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "submission": submission,
                        "previous_submissions": previous_submissions
                    }
                )
            except Exception as e:
                print(f"Database error in submission detail: {e}")
                return RedirectResponse(url="/exercises/submissions")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in submission detail: {e}")
            return RedirectResponse(url="/Therapist_Login")


    @app.get("/exercises/patient-submissions/{patient_id}")
    async def patient_exercise_submissions(request: Request, patient_id: int):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:

                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image
                    FROM Therapists 
                    WHERE id = %s""", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()

                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")


                cursor.execute(
                    """SELECT * FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return RedirectResponse(url="/patients")


                cursor.execute(
                    """SELECT evs.*, e.name as exercise_name, tp.name as plan_name
                    FROM ExerciseVideoSubmissions evs
                    JOIN Exercises e ON evs.exercise_id = e.exercise_id
                    JOIN TreatmentPlans tp ON evs.treatment_plan_id = tp.plan_id
                    WHERE evs.patient_id = %s
                    ORDER BY evs.submission_date DESC""",
                    (patient_id,)
                )
                submissions = cursor.fetchall()


                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0

                return templates.TemplateResponse(
                    "dist/exercises/patient_submissions.html",
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "patient": patient,
                        "submissions": submissions
                    }
                )

            except Exception as e:
                print(f"Database error in patient exercise submissions: {e}")
                return RedirectResponse(url="/patients")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in patient exercise submissions: {e}")
            return RedirectResponse(url="/Therapist_Login")
                
    @app.get("/exercises/{exercise_id}/edit")
    async def edit_exercise_form(
        request: Request,
        exercise_id: int,
        user = Depends(get_current_user)
    ):
        """Route to display the edit exercise form"""
        db = get_Mysql_db()
        cursor = None
        
        try:
            cursor = db.cursor(dictionary=True)
            
            cursor.execute("SELECT * FROM Exercises WHERE exercise_id = %s", (exercise_id,))
            exercise = cursor.fetchone()
            
            if not exercise:
                return RedirectResponse(url="/exercises", status_code=303)
            
            cursor.execute("SELECT * FROM ExerciseCategories ORDER BY name")
            categories = cursor.fetchall()
            
            therapist_data = await get_therapist_data(user["user_id"])
            
            return templates.TemplateResponse(
                "dist/exercises/edit_exercise.html", 
                {
                    "request": request,
                    "exercise": exercise,
                    "categories": categories,
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"]
                }
            )
        except Exception as e:
            print(f"Error loading edit exercise form: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/exercises", status_code=303)
        finally:
            if cursor:
                cursor.close()
            if db:
                db.close()

    @app.post("/exercises/{exercise_id}/edit")
    async def update_exercise(
        request: Request,
        exercise_id: int,
        name: str = Form(...),
        category_id: Optional[int] = Form(None),
        description: Optional[str] = Form(None),
        difficulty: Optional[str] = Form(None),
        duration: Optional[int] = Form(None),
        instructions: Optional[str] = Form(None),
        keep_current_video: Optional[str] = Form(None),
        video_source: Optional[str] = Form(None),
        video_url: Optional[str] = Form(None),
        video_upload: Optional[UploadFile] = File(None),
        user = Depends(get_current_user)
    ):
        """Route to handle updating an exercise"""
        db = get_Mysql_db()
        cursor = None
        
        try:
            cursor = db.cursor(dictionary=True)
            
            cursor.execute("SELECT * FROM Exercises WHERE exercise_id = %s", (exercise_id,))
            exercise = cursor.fetchone()
            
            if not exercise:
                return RedirectResponse(url="/exercises")
            

            final_video_url = exercise['video_url']
            video_type = exercise.get('video_type', 'none')
            video_size = exercise.get('video_size', None)  
            video_filename = exercise.get('video_filename', None) 
            

            if not keep_current_video:
                if video_source == 'youtube' and video_url:
                    final_video_url = video_url
                    video_type = 'youtube'
                    video_size = None
                    video_filename = None
                    
                elif video_source == 'upload' and video_upload and video_upload.filename:

                    current_file = Path(__file__).resolve()
                    project_root = current_file.parent.parent.parent
                    uploads_dir = project_root / "Frontend_Web" / "static" / "assets" / "videos" / "exercises"
                    uploads_dir.mkdir(parents=True, exist_ok=True)
                    

                    video_filename = video_upload.filename
                    

                    file_extension = video_filename.split(".")[-1].lower()
                    unique_filename = f"exercise_{exercise_id}_{int(time.time())}_{secrets.token_hex(4)}.{file_extension}"
                    file_path = uploads_dir / unique_filename
                    

                    video_content = await video_upload.read()
                    video_size = len(video_content)
                    

                    async with aiofiles.open(file_path, "wb") as f:
                        await f.write(video_content)
                    

                    old_video_url = exercise['video_url']
                    old_video_type = exercise.get('video_type', '')
                    
                    if old_video_url and old_video_type == 'upload':
                        try:
                            old_video_path = Path(project_root) / "Frontend_Web" / old_video_url.lstrip('/')
                            if os.path.exists(old_video_path):
                                os.remove(old_video_path)
                                print(f"Deleted old video: {old_video_path}")
                        except Exception as e:
                            print(f"Error deleting old video: {e}")
                    

                    final_video_url = f"/static/assets/videos/exercises/{unique_filename}"
                    video_type = 'upload'
                else:

                    final_video_url = None
                    video_type = 'none'
                    video_size = None
                    video_filename = None
            

            cursor.execute(
                """UPDATE Exercises 
                SET name = %s, category_id = %s, description = %s, 
                    video_url = %s, video_type = %s, video_size = %s, video_filename = %s,
                    difficulty = %s, duration = %s, instructions = %s, 
                    updated_at = CURRENT_TIMESTAMP
                WHERE exercise_id = %s""",
                (name, category_id, description, final_video_url, video_type, video_size, 
                video_filename, difficulty, duration, instructions, exercise_id)
            )
            db.commit()
            
            return RedirectResponse(url=f"/exercises", status_code=303)
        except Exception as e:
            if db:
                db.rollback()
            print(f"Error updating exercise: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            
            categories = []
            try:
                cursor.execute("SELECT * FROM ExerciseCategories ORDER BY name")
                categories = cursor.fetchall()
            except:
                pass
            
            therapist_data = await get_therapist_data(user["user_id"])
            
            return templates.TemplateResponse(
                "dist/exercises/edit_exercise.html", 
                {
                    "request": request,
                    "exercise": exercise,
                    "categories": categories,
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"],
                    "error": f"Error updating exercise: {str(e)}"
                },
                status_code=400
            )
        finally:
            if cursor:
                cursor.close()
            if db:
                db.close()

    @app.post("/exercises/delete")
    async def delete_exercise(
        request: Request,
        exercise_id: int = Form(...),
        user = Depends(get_current_user)
    ):
        """Route to delete an exercise"""
        db = get_Mysql_db()
        cursor = None
        
        try:
            cursor = db.cursor(dictionary=True)
            
            cursor.execute(
                "SELECT video_url, video_type FROM Exercises WHERE exercise_id = %s", 
                (exercise_id,)
            )
            exercise = cursor.fetchone()
            
            if exercise and exercise['video_url'] and exercise.get('video_type') == 'upload':
                try:
                    current_file = Path(__file__).resolve()
                    project_root = current_file.parent.parent.parent
                    video_path = project_root / "Frontend_Web" / exercise['video_url'].lstrip('/')
                    
                    if os.path.exists(video_path):
                        os.remove(video_path)
                        print(f"Deleted video file: {video_path}")
                except Exception as e:
                    print(f"Error deleting video file: {e}")
            
            cursor.execute(
                "DELETE FROM Exercises WHERE exercise_id = %s", 
                (exercise_id,)
            )
            db.commit()
            
            return RedirectResponse(url="/exercises", status_code=303)
        except Exception as e:
            if db:
                db.rollback()
            print(f"Error deleting exercise: {e}")
            return RedirectResponse(url="/exercises", status_code=303)
        finally:
            if cursor:
                cursor.close()
            if db:
                db.close()
            
    @app.get("/patients")
    async def get_patients_page(request: Request, user=Depends(get_current_user)):
        db = get_Mysql_db()
        cursor = db.cursor(dictionary=True)

        try:
            cursor.execute(
                "SELECT * FROM Patients WHERE therapist_id = %s ORDER BY last_name", 
                (user["user_id"],)
            )
            patients = cursor.fetchall()

            therapist_data = await get_therapist_data(user["user_id"])

            return templates.TemplateResponse(
                "dist/dashboard/patient_directory.html", 
                {
                    "request": request,
                    "patients": patients,
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"]
                }
            )
        finally:
            cursor.close()
            db.close()

    @app.get("/patients/add")
    async def add_patient_page(request: Request, user=Depends(get_current_user)):
        therapist_data = await get_therapist_data(user["user_id"])

        return templates.TemplateResponse(
            "dist/dashboard/add_patient.html", 
            {
                "request": request,
                "therapist": therapist_data,
                "first_name": therapist_data["first_name"],
                "last_name": therapist_data["last_name"]
            }
        )

    @app.post("/patients/add")
    async def add_patient(
        request: Request,
        first_name: str = Form(...),
        last_name: str = Form(...),
        email: str = Form(None),
        phone: str = Form(None),
        date_of_birth: str = Form(None),
        address: str = Form(None),
        diagnosis: str = Form(None),
        notes: str = Form(None),
        user=Depends(get_current_user)
    ):
        db = get_Mysql_db()
        cursor = db.cursor()

        try:
            cursor.execute(
                """INSERT INTO Patients 
                (therapist_id, first_name, last_name, email, phone, date_of_birth, 
                address, diagnosis, notes) 
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)""",
                (user["user_id"], first_name, last_name, email, phone, 
                date_of_birth, address, diagnosis, notes)
            )
            db.commit()
            return RedirectResponse(url="/patients", status_code=303)
        except Exception as e:
            print(f"Error adding patient: {e}")
            therapist_data = await get_therapist_data(user["user_id"])
            return templates.TemplateResponse(
                "dist/dashboard/add_patient.html", 
                {
                    "request": request,
                    "error": f"Error adding patient: {str(e)}",
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"],
                    "today": datetime.datetime.now()
                }
            )
        finally:
            cursor.close()
            db.close()

    @app.get("/patients/{patient_id}")
    async def patient_details(request: Request, patient_id: int):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:

                now = datetime.datetime.now()
                today = datetime.date.today()
                
                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image
                    FROM Therapists 
                    WHERE id = %s""", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()

                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")


                cursor.execute(
                    """SELECT * FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                patient = cursor.fetchone()

                if not patient:
                    return RedirectResponse(url="/patients")


                cursor.execute(
                    """SELECT * FROM TreatmentPlans
                    WHERE patient_id = %s
                    ORDER BY created_at DESC""",
                    (patient_id,)
                )
                treatment_plans = cursor.fetchall()


                cursor.execute(
                    """SELECT * FROM Appointments
                    WHERE patient_id = %s
                    ORDER BY appointment_date DESC, appointment_time DESC""",
                    (patient_id,)
                )
                appointments_raw = cursor.fetchall()
                appointments = []
                
                for appt in appointments_raw:
                    processed_appt = {}
                    
                    for key, value in appt.items():
                        if isinstance(value, datetime.datetime):
                            processed_appt[key] = value
                        elif isinstance(value, datetime.date):
                            processed_appt[key] = value
                        elif isinstance(value, datetime.timedelta):
                            processed_appt[key] = value
                        else:
                            processed_appt[key] = value
                    
                    appointments.append(processed_appt)


                cursor.execute(
                    """SELECT * FROM PatientMetrics
                    WHERE patient_id = %s
                    ORDER BY measurement_date DESC""",
                    (patient_id,)
                )
                metrics = cursor.fetchall()


                cursor.execute(
                    """SELECT 
                        pn.note_id, 
                        pn.patient_id, 
                        pn.therapist_id, 
                        pn.appointment_id, 
                        pn.note_text,
                        pn.created_at,
                        pn.updated_at,
                        t.first_name,
                        t.last_name
                    FROM PatientNotes pn
                    LEFT JOIN Therapists t ON pn.therapist_id = t.id
                    WHERE pn.patient_id = %s
                    ORDER BY pn.created_at DESC""",
                    (patient_id,)
                )
                

                patient_notes_raw = cursor.fetchall()
                patient_notes = []
                

                for note in patient_notes_raw:

                    processed_note = {}
                    

                    for key, value in note.items():

                        if isinstance(value, datetime.datetime):
                            processed_note[key] = value.strftime('%Y-%m-%d %H:%M:%S')
                        elif isinstance(value, datetime.date):
                            processed_note[key] = value.strftime('%Y-%m-%d')
                        elif isinstance(value, datetime.timedelta):
                            total_seconds = int(value.total_seconds())
                            hours, remainder = divmod(total_seconds, 3600)
                            minutes, seconds = divmod(remainder, 60)
                            processed_note[key] = f"{hours:02d}:{minutes:02d}"
                        else:
                            processed_note[key] = value
                    

                    patient_notes.append(processed_note)
                

                appointment_ids = [note['appointment_id'] for note in patient_notes 
                                if note.get('appointment_id') is not None]
                
                if appointment_ids:

                    placeholders = ', '.join(['%s'] * len(appointment_ids))
                    cursor.execute(
                        f"""SELECT 
                            appointment_id, 
                            appointment_date, 
                            appointment_time 
                        FROM Appointments 
                        WHERE appointment_id IN ({placeholders})""",
                        tuple(appointment_ids)
                    )
                    
                    appointments_data = cursor.fetchall()
                    appointments_dict = {}
                    

                    for appt in appointments_data:
                        appt_date = appt['appointment_date']
                        appt_time = appt['appointment_time']
                        

                        if isinstance(appt_date, (datetime.date, datetime.datetime)):
                            date_str = appt_date.strftime('%Y-%m-%d')
                        else:
                            date_str = str(appt_date)
                            
                        if isinstance(appt_time, datetime.timedelta):
                            total_seconds = int(appt_time.total_seconds())
                            hours, remainder = divmod(total_seconds, 3600)
                            minutes, seconds = divmod(remainder, 60)
                            time_str = f"{hours:02d}:{minutes:02d}"
                        else:
                            time_str = str(appt_time)
                        
                        appointments_dict[appt['appointment_id']] = {
                            'appointment_date': date_str,
                            'appointment_time': time_str
                        }
                    

                    for note in patient_notes:
                        if note.get('appointment_id') in appointments_dict:
                            note['appointment_date'] = appointments_dict[note['appointment_id']]['appointment_date']
                            note['appointment_time'] = appointments_dict[note['appointment_id']]['appointment_time']


                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0


                print(f"Patient ID: {patient_id}")
                print(f"Patient notes count: {len(patient_notes)}")
                if patient_notes:
                    print(f"First note: {patient_notes[0]['note_text'][:50]}...")

                return templates.TemplateResponse(
                    "dist/dashboard/patient_details.html",  
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "patient": patient,
                        "treatment_plans": treatment_plans,
                        "appointments": appointments,
                        "metrics": metrics,
                        "patient_notes": patient_notes, 
                        "today": today,
                        "now": now  
                    }
                )

            except Exception as e:
                print(f"Database error in patient details: {e}")
                print(traceback.format_exc())  
                return RedirectResponse(url="/patients")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in patient details: {e}")
            print(traceback.format_exc()) 
            return RedirectResponse(url="/Therapist_Login")
        
    @app.get("/patients/{patient_id}/edit")
    async def edit_patient_page(request: Request, patient_id: int, user=Depends(get_current_user)):
        """Route to display the edit patient form"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    "SELECT first_name, last_name FROM Therapists WHERE id = %s", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")
                
                cursor.execute(
                    "SELECT * FROM Patients WHERE patient_id = %s AND therapist_id = %s", 
                    (patient_id, session_data["user_id"])
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return RedirectResponse(url="/patients?error=not_found")
                
                if patient['date_of_birth'] and isinstance(patient['date_of_birth'], datetime.date):
                    patient['formatted_dob'] = patient['date_of_birth'].strftime('%Y-%m-%d')
                else:
                    patient['formatted_dob'] = None
                
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                
                base_url = request.url.scheme + "://" + request.url.netloc
                therapist_data = await get_therapist_data(user["user_id"])
                
                return templates.TemplateResponse(
                    "dist/dashboard/edit_patient.html",
                    {
                        "request": request,
                        "therapist": therapist_data,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "patient": patient,
                        "unread_messages_count": unread_messages_count,
                        "base_url": base_url  
                    }
                )
            except Exception as e:
                print(f"Database error in edit patient page: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/patients")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in edit patient page: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")

    @app.post("/patients/{patient_id}/edit")
    async def update_patient(request: Request, patient_id: int, user=Depends(get_current_user)):
        """Handle patient update form submission"""
        session_id = request.cookies.get("session_id")
        therapist_data = await get_therapist_data(user["user_id"])
        
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            form_data = await request.form()
            
            first_name = form_data.get("first_name")
            last_name = form_data.get("last_name")
            email = form_data.get("email")
            phone = form_data.get("phone")
            date_of_birth = form_data.get("date_of_birth")
            diagnosis = form_data.get("diagnosis")
            address = form_data.get("address")
            notes = form_data.get("notes")
            status = form_data.get("status", "Active")
            
            if not first_name or not last_name:
                return RedirectResponse(
                    url=f"/patients/{patient_id}/edit?error=missing_fields", 
                    status_code=303
                )

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor()
                
                cursor.execute(
                    """SELECT patient_id 
                    FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    print(f"Patient {patient_id} does not belong to therapist {session_data['user_id']}")
                    return RedirectResponse(url="/patients?error=unauthorized")
                
                cursor.execute(
                    """UPDATE Patients 
                    SET first_name = %s, 
                        last_name = %s, 
                        email = %s, 
                        phone = %s, 
                        date_of_birth = %s, 
                        diagnosis = %s,
                        address = %s,
                        notes = %s,
                        status = %s,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE patient_id = %s""",
                    (
                        first_name, last_name, email, phone, 
                        date_of_birth if date_of_birth else None, 
                        diagnosis, address, notes, status, patient_id
                    )
                )
                db.commit()
                
                return RedirectResponse(url=f"/patients/{patient_id}?success=updated", status_code=303)
                    
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error updating patient: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url=f"/patients/{patient_id}/edit?error=db_error")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error updating patient: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")
        
    @app.get("/treatment-plans/new")
    async def new_treatment_plan_page(request: Request, user=Depends(get_current_user)):
        db = get_Mysql_db()
        cursor = db.cursor(dictionary=True)

        try:
            cursor.execute(
                "SELECT patient_id, first_name, last_name FROM Patients WHERE therapist_id = %s", 
                (user["user_id"],)
            )
            patients = cursor.fetchall()

            cursor.execute("SELECT * FROM Exercises")
            exercises = cursor.fetchall()

            therapist_data = await get_therapist_data(user["user_id"])
            print(f"exercises: {exercises}")

            return templates.TemplateResponse(
                "dist/treatment_plans/new_plan.html", 
                {
                    "request": request,
                    "patients": patients,
                    "exercises": exercises,
                    "therapist":therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"],
                }
            )
        finally:
            cursor.close()
            db.close()

    async def get_therapist_data(therapist_id):
        db = get_Mysql_db()
        cursor = db.cursor(dictionary=True)

        try:
            cursor.execute(
                "SELECT first_name, last_name, profile_image FROM Therapists WHERE id = %s", 
                (therapist_id,)
            )
            return cursor.fetchone()
        finally:
            cursor.close()
            db.close()
        
    @app.get("/treatment-plans/{plan_id}")
    async def view_treatment_plan(request: Request, plan_id: int):
        """Display detailed view of a specific treatment plan with exercises and progress"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT id, first_name, last_name, profile_image FROM Therapists WHERE id = %s", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")
                

                cursor.execute(
                    """SELECT tp.*, p.first_name as patient_first_name, p.last_name as patient_last_name 
                    FROM TreatmentPlans tp
                    JOIN Patients p ON tp.patient_id = p.patient_id
                    WHERE tp.plan_id = %s AND tp.therapist_id = %s""", 
                    (plan_id, session_data["user_id"])
                )
                plan = cursor.fetchone()
                
                if not plan:
                    return RedirectResponse(url="/treatment-plans?error=not_found")
                

                cursor.execute(
                    "SELECT * FROM Patients WHERE patient_id = %s AND therapist_id = %s",
                    (plan["patient_id"], session_data["user_id"])
                )
                patient = cursor.fetchone()
                

                cursor.execute(
                    """SELECT tpe.*, e.name as exercise_name, e.difficulty, e.duration as exercise_duration,
                            e.video_url, e.video_type, e.instructions
                    FROM TreatmentPlanExercises tpe
                    JOIN Exercises e ON tpe.exercise_id = e.exercise_id
                    WHERE tpe.plan_id = %s
                    ORDER BY tpe.plan_exercise_id""",
                    (plan_id,)
                )
                plan_exercises = cursor.fetchall()
                

                for exercise in plan_exercises:
                    if exercise["notes"]:
                        exercise["notes"] = exercise["notes"]
                    elif exercise["instructions"]:
                        exercise["notes"] = exercise["instructions"]
                    else:
                        exercise["notes"] = "No specific instructions provided for this exercise."
                

                cursor.execute(
                    """SELECT * FROM PatientMetrics 
                    WHERE patient_id = %s 
                    ORDER BY measurement_date DESC
                    LIMIT 1""",
                    (plan["patient_id"],)
                )
                patient_progress = cursor.fetchone()
                

                if not patient_progress:
                    patient_progress = {
                        'adherence_rate': 0,
                        'recovery_progress': 0,
                        'functionality_score': 0
                    }
                

                cursor.execute(
                    """SELECT 
                        CASE 
                            WHEN (
                                CASE 
                                    WHEN repetitions IS NOT NULL THEN (repetitions_completed / repetitions) * 100
                                    ELSE (sets_completed / sets) * 100
                                END
                            ) >= 90 THEN 'complete'
                            WHEN (
                                CASE 
                                    WHEN repetitions IS NOT NULL THEN (repetitions_completed / repetitions) * 100
                                    ELSE (sets_completed / sets) * 100
                                END
                            ) >= 50 THEN 'partial'
                            ELSE 'missed'
                        END as status,
                        COUNT(*) as count
                    FROM PatientExerciseProgress pep
                    JOIN TreatmentPlanExercises tpe ON pep.plan_exercise_id = tpe.plan_exercise_id
                    WHERE tpe.plan_id = %s
                    GROUP BY status""",
                    (plan_id,)
                )
                exercise_completion_raw = cursor.fetchall()
                

                exercise_completion = {
                    'complete_count': 0, 
                    'partial_count': 0, 
                    'missed_count': 0,
                    'complete_percentage': 0,
                    'partial_percentage': 0,
                    'missed_percentage': 0
                }
                
                total_exercises = 0
                for ec in exercise_completion_raw:
                    total_exercises += ec['count']
                    if ec['status'] == 'complete':
                        exercise_completion['complete_count'] = ec['count']
                    elif ec['status'] == 'partial':
                        exercise_completion['partial_count'] = ec['count']
                    elif ec['status'] == 'missed':
                        exercise_completion['missed_count'] = ec['count']
                
                if total_exercises > 0:
                    exercise_completion['complete_percentage'] = round((exercise_completion['complete_count'] / total_exercises) * 100)
                    exercise_completion['partial_percentage'] = round((exercise_completion['partial_count'] / total_exercises) * 100)
                    exercise_completion['missed_percentage'] = round((exercise_completion['missed_count'] / total_exercises) * 100)
                

                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                
                return templates.TemplateResponse(
                    "dist/treatment_plans/view_plan.html",
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "plan": plan,
                        "patient": patient,
                        "plan_exercises": plan_exercises,
                        "patient_progress": patient_progress,
                        "exercise_completion": exercise_completion,
                        "unread_messages_count": unread_messages_count
                    }
                )
                    
            except Exception as e:
                print(f"Database error in view treatment plan: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/treatment-plans?error=database")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in view treatment plan: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")
            
    @app.get("/treatment-plans/{plan_id}/edit")
    async def edit_treatment_plan_form(request: Request, plan_id: int):
        """Route to display the edit treatment plan form"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    """SELECT tp.*, p.first_name as patient_first_name, p.last_name as patient_last_name
                    FROM TreatmentPlans tp
                    JOIN Patients p ON tp.patient_id = p.patient_id
                    WHERE tp.plan_id = %s AND tp.therapist_id = %s""",
                    (plan_id, session_data["user_id"])
                )
                plan = cursor.fetchone()
                
                if not plan:
                    return RedirectResponse(url="/treatment-plans", status_code=303)
                

                cursor.execute(
                    """SELECT tpe.*, e.name as exercise_name, e.difficulty, e.duration as exercise_duration
                    FROM TreatmentPlanExercises tpe
                    JOIN Exercises e ON tpe.exercise_id = e.exercise_id
                    WHERE tpe.plan_id = %s
                    ORDER BY tpe.plan_exercise_id""",
                    (plan_id,)
                )
                plan_exercises = cursor.fetchall()
                

                cursor.execute(
                    "SELECT patient_id, first_name, last_name FROM Patients WHERE therapist_id = %s",
                    (session_data["user_id"],)
                )
                patients = cursor.fetchall()
                

                cursor.execute("SELECT * FROM Exercises ORDER BY name")
                exercises = cursor.fetchall()
                

                therapist_data = await get_therapist_data(session_data["user_id"])
                
                return templates.TemplateResponse(
                    "dist/treatment_plans/edit_plan.html",
                    {
                        "request": request,
                        "plan": plan,
                        "plan_exercises": plan_exercises,
                        "patients": patients,
                        "exercises": exercises,
                        "therapist": therapist_data,
                        "first_name": therapist_data["first_name"],
                        "last_name": therapist_data["last_name"]
                    }
                )
            except Exception as e:
                print(f"Error loading edit treatment plan form: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/treatment-plans", status_code=303)
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Unexpected error in edit treatment plan form: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/front-page", status_code=303)

    @app.post("/treatment-plans/{plan_id}/edit")
    async def update_treatment_plan(request: Request, plan_id: int):
        """Route to handle updating a treatment plan"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            

            form = await request.form()
            print("RECEIVED FORM DATA FOR UPDATE:", dict(form))
            

            patient_id = form.get("patient_id")
            plan_name = form.get("plan_name")
            description = form.get("description", "")
            start_date = form.get("start_date")
            end_date = form.get("end_date")
            status = form.get("status", "Active")
            
            print(f"Plan update details: ID={plan_id}, patient={patient_id}, name={plan_name}")
            

            if not patient_id or not plan_name or not start_date:
                print("Missing required fields in update")
                return RedirectResponse(f"/treatment-plans/{plan_id}/edit?error=missing_fields", status_code=303)
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor()
                

                cursor.execute(
                    "SELECT plan_id FROM TreatmentPlans WHERE plan_id = %s AND therapist_id = %s",
                    (plan_id, session_data["user_id"])
                )
                if not cursor.fetchone():
                    return RedirectResponse(url="/treatment-plans", status_code=303)
                

                cursor.execute(
                    """UPDATE TreatmentPlans 
                    SET patient_id = %s, name = %s, description = %s, 
                        start_date = %s, end_date = %s, status = %s
                    WHERE plan_id = %s""",
                    (patient_id, plan_name, description, start_date, end_date, status, plan_id)
                )
                

                existing_exercise_ids = form.getlist("existing_exercise_id")
                keep_exercises = form.getlist("keep_exercise")
                

                cursor.execute(
                    "SELECT plan_exercise_id FROM TreatmentPlanExercises WHERE plan_id = %s",
                    (plan_id,)
                )
                current_exercise_ids = [row[0] for row in cursor.fetchall()]
                

                for ex_id in current_exercise_ids:
                    if str(ex_id) not in keep_exercises:
                        cursor.execute(
                            "DELETE FROM TreatmentPlanExercises WHERE plan_exercise_id = %s",
                            (ex_id,)
                        )
                        print(f"Deleted exercise ID: {ex_id}")
                

                for i, ex_id in enumerate(keep_exercises):
                    if not ex_id:
                        continue
                        
                    prefix = f"existing_{ex_id}_"
                    ex_sets = form.get(f"{prefix}sets")
                    ex_reps = form.get(f"{prefix}repetitions")
                    ex_freq = form.get(f"{prefix}frequency")
                    ex_duration = form.get(f"{prefix}duration")
                    ex_notes = form.get(f"{prefix}notes")
                    
                    cursor.execute(
                        """UPDATE TreatmentPlanExercises
                        SET sets = %s, repetitions = %s, frequency = %s, 
                            duration = %s, notes = %s
                        WHERE plan_exercise_id = %s""",
                        (ex_sets, ex_reps, ex_freq, ex_duration, ex_notes, ex_id)
                    )
                    print(f"Updated exercise ID: {ex_id}")
                

                new_exercises = form.getlist("new_exercise_id")
                new_sets = form.getlist("new_sets")
                new_reps = form.getlist("new_repetitions")
                new_freq = form.getlist("new_frequency")
                new_duration = form.getlist("new_duration")
                new_notes = form.getlist("new_notes")
                
                for i, ex_id in enumerate(new_exercises):
                    if not ex_id or ex_id == "":
                        continue
                        
                    ex_sets = new_sets[i] if i < len(new_sets) else None
                    ex_reps = new_reps[i] if i < len(new_reps) else None
                    ex_freq = new_freq[i] if i < len(new_freq) else None
                    ex_duration = new_duration[i] if i < len(new_duration) else None
                    ex_notes = new_notes[i] if i < len(new_notes) else None
                    
                    cursor.execute(
                        """INSERT INTO TreatmentPlanExercises
                        (plan_id, exercise_id, sets, repetitions, frequency, duration, notes)
                        VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                        (plan_id, ex_id, ex_sets, ex_reps, ex_freq, ex_duration, ex_notes)
                    )
                    print(f"Added new exercise ID: {ex_id}")
                
                db.commit()
                print(f"Treatment plan {plan_id} updated successfully")
                
                return RedirectResponse(url=f"/treatment-plans", status_code=303)
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error in update treatment plan: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(f"/treatment-plans/{plan_id}/edit?error=db_error", status_code=303)
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Unexpected error in update treatment plan: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/front-page", status_code=303)

    @app.post("/treatment-plans/delete")
    async def delete_treatment_plan(request: Request):
        """Route to delete a treatment plan"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            

            form = await request.form()
            plan_id_str = form.get("plan_id")
            
            if not plan_id_str:
                return RedirectResponse(url="/treatment-plans?error=no_plan_id", status_code=303)
            
            try:
                plan_id = int(plan_id_str)
            except ValueError:
                return RedirectResponse(url="/treatment-plans?error=invalid_plan_id", status_code=303)
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor()
                

                cursor.execute(
                    "SELECT plan_id FROM TreatmentPlans WHERE plan_id = %s AND therapist_id = %s",
                    (plan_id, session_data["user_id"])
                )
                if not cursor.fetchone():
                    return RedirectResponse(url="/treatment-plans?error=not_found", status_code=303)
                
                cursor.execute(
                    "DELETE FROM TreatmentPlanExercises WHERE plan_id = %s",
                    (plan_id,)
                )
                
                cursor.execute(
                    "DELETE FROM TreatmentPlans WHERE plan_id = %s",
                    (plan_id,)
                )
                
                db.commit()
                print(f"Treatment plan {plan_id} deleted successfully")
                
                return RedirectResponse(url="/treatment-plans?success=deleted", status_code=303)
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error in delete treatment plan: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/treatment-plans?error=db_error", status_code=303)
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Unexpected error in delete treatment plan: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/front-page", status_code=303)
    
    @app.get("/appointments/new")
    async def new_appointment_form(request: Request, user=Depends(get_current_user)):
        """Display the new appointment form"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    "SELECT first_name, last_name FROM Therapists WHERE id = %s", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")
                
                cursor.execute(
                    """SELECT patient_id, first_name, last_name, diagnosis, phone
                    FROM Patients 
                    WHERE therapist_id = %s
                    ORDER BY last_name, first_name""", 
                    (session_data["user_id"],)
                )
                patients = cursor.fetchall()
                
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                
                cursor.execute(
                    """SELECT m.message_id, m.subject, m.content, m.created_at, 
                            t.first_name, t.last_name, COALESCE(t.profile_image, 'avatar-1.jpg') as profile_image
                        FROM Messages m
                        JOIN Therapists t ON m.sender_id = t.id
                        WHERE m.recipient_id = %s AND m.is_read = FALSE
                        ORDER BY m.created_at DESC
                        LIMIT 4""",
                    (session_data["user_id"],)
                )
                messages_result = cursor.fetchall()

                recent_messages = []
                for message in messages_result:
                    message_with_time = message.copy()
                    
                    timestamp = message['created_at']
                    now = datetime.datetime.now()
                    if isinstance(timestamp, datetime.datetime):
                        diff = now - timestamp
                        if timestamp.date() == now.date():
                            message_with_time['time_display'] = timestamp.strftime('%I:%M %p')
                            
                            minutes_ago = diff.seconds // 60
                            if minutes_ago < 60:
                                message_with_time['time_ago'] = f"{minutes_ago} min ago"
                            else:
                                hours_ago = minutes_ago // 60
                                message_with_time['time_ago'] = f"{hours_ago} hours ago"
                                
                        elif timestamp.date() == (now - timedelta(days=1)).date():
                            message_with_time['time_display'] = "Yesterday"
                            message_with_time['time_ago'] = timestamp.strftime('%I:%M %p')
                        else:
                            message_with_time['time_display'] = timestamp.strftime('%d %b')
                            message_with_time['time_ago'] = timestamp.strftime('%Y')
                            
                    recent_messages.append(message_with_time)
                
                today = datetime.datetime.now().strftime('%Y-%m-%d')
                therapist_data = await get_therapist_data(user["user_id"])

                
                return templates.TemplateResponse(
                    "dist/appointments/new_appointment.html",
                    {
                        "request": request,
                        "therapist": therapist_data,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "patients": patients,
                        "unread_messages_count": unread_messages_count,
                        "recent_messages": recent_messages,
                        "today": today
                    }
                )
            except Exception as e:
                print(f"Database error in new appointment form: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/appointments")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in new appointment form: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")

    def process_appointment_for_calendar(appointment):
        """Process an appointment object to make it suitable for calendar display"""
        from datetime import datetime, date, time, timedelta 
        
        processed = dict(appointment)
        

        if 'appointment_id' in processed and processed['appointment_id'] is not None:
            processed['appointment_id'] = int(processed['appointment_id'])
        

        if 'appointment_date' in processed and processed['appointment_date'] is not None:
            if isinstance(processed['appointment_date'], datetime) or isinstance(processed['appointment_date'], date):
                processed['appointment_date_iso'] = processed['appointment_date'].isoformat()
        

        if 'appointment_time' in processed and processed['appointment_time'] is not None:
            if isinstance(processed['appointment_time'], timedelta):
                total_seconds = processed['appointment_time'].total_seconds()
                hours = int(total_seconds // 3600)
                minutes = int((total_seconds % 3600) // 60)
                
                processed['appointment_time_obj'] = {
                    'hour': hours,
                    'minute': minutes
                }

                processed['appointment_time_24h'] = f"{hours:02d}:{minutes:02d}"

                am_pm = "AM" if hours < 12 else "PM"
                display_hours = hours if hours <= 12 else hours - 12
                display_hours = 12 if display_hours == 0 else display_hours
                processed['appointment_time_12h'] = f"{display_hours}:{minutes:02d} {am_pm}"
                
                end_hours = hours + ((processed.get('duration', 60) + minutes) // 60)
                end_minutes = (minutes + processed.get('duration', 60)) % 60
                processed['end_time_24h'] = f"{end_hours:02d}:{end_minutes:02d}"

                processed['formatted_time'] = processed['appointment_time_12h']
            
            elif hasattr(processed['appointment_time'], 'hour'):

                hours = processed['appointment_time'].hour
                minutes = processed['appointment_time'].minute
                
                processed['appointment_time_obj'] = {
                    'hour': hours,
                    'minute': minutes
                }
                
                processed['appointment_time_24h'] = f"{hours:02d}:{minutes:02d}"

                am_pm = "AM" if hours < 12 else "PM"
                display_hours = hours if hours <= 12 else hours - 12
                display_hours = 12 if display_hours == 0 else display_hours
                processed['appointment_time_12h'] = f"{display_hours}:{minutes:02d} {am_pm}"
                
                end_hours = hours + ((processed.get('duration', 60) + minutes) // 60)
                end_minutes = (minutes + processed.get('duration', 60)) % 60
                processed['end_time_24h'] = f"{end_hours:02d}:{end_minutes:02d}"
                

                processed['formatted_time'] = processed['appointment_time_12h']

        if 'duration' not in processed or processed['duration'] is None:
            processed['duration'] = 60

        if 'status' not in processed or processed['status'] is None:
            processed['status'] = 'Scheduled'
        
        return processed

    def serialize_datetime(obj):
        """JSON serializer for datetime objects not serializable by default json code"""
        from datetime import datetime, date, time, timedelta
        
        if isinstance(obj, (datetime, date)):
            return obj.isoformat()
        elif isinstance(obj, time):
            return obj.strftime('%H:%M:%S')
        elif isinstance(obj, timedelta):
            total_seconds = obj.total_seconds()
            hours = int(total_seconds // 3600)
            minutes = int((total_seconds % 3600) // 60)
            return f"{hours:02d}:{minutes:02d}"
        raise TypeError(f"Type {type(obj)} not serializable")
    
    @app.get("/appointments")
    async def appointments_page(request: Request, user=Depends(get_current_user)):
        """Route to display appointments schedule and management page"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    "SELECT first_name, last_name FROM Therapists WHERE id = %s", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")
                
                cursor.execute(
                    """SELECT a.*, p.first_name as patient_first_name, p.last_name as patient_last_name 
                    FROM Appointments a
                    JOIN Patients p ON a.patient_id = p.patient_id
                    WHERE a.therapist_id = %s AND a.appointment_date >= CURDATE()
                    ORDER BY a.appointment_date, a.appointment_time""", 
                    (session_data["user_id"],)
                )
                upcoming_appointments_raw = cursor.fetchall()

                cursor.execute(
                    """SELECT a.*, p.first_name as patient_first_name, p.last_name as patient_last_name 
                    FROM Appointments a
                    JOIN Patients p ON a.patient_id = p.patient_id
                    WHERE a.therapist_id = %s AND a.appointment_date < CURDATE()
                    ORDER BY a.appointment_date DESC, a.appointment_time DESC
                    LIMIT 10""", 
                    (session_data["user_id"],)
                )
                past_appointments_raw = cursor.fetchall()
                
                cursor.execute(
                    "SELECT patient_id, first_name, last_name, diagnosis FROM Patients WHERE therapist_id = %s", 
                    (session_data["user_id"],)
                )
                patients = cursor.fetchall()
                
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                
                cursor.execute(
                    """SELECT m.message_id, m.subject, m.content, m.created_at, 
                            t.first_name, t.last_name, COALESCE(t.profile_image, 'avatar-1.jpg') as profile_image
                        FROM Messages m
                        JOIN Therapists t ON m.sender_id = t.id
                        WHERE m.recipient_id = %s AND m.is_read = FALSE
                        ORDER BY m.created_at DESC
                        LIMIT 4""",
                    (session_data["user_id"],)
                )
                messages_result = cursor.fetchall()

                recent_messages = []
                for message in messages_result:
                    message_with_time = message.copy()
                    
                    timestamp = message['created_at']
                    now = datetime.datetime.now()
                    if isinstance(timestamp, datetime):
                        diff = now - timestamp
                        if timestamp.date() == now.date():
                            message_with_time['time_display'] = timestamp.strftime('%I:%M %p')
                            
                            minutes_ago = diff.seconds // 60
                            if minutes_ago < 60:
                                message_with_time['time_ago'] = f"{minutes_ago} min ago"
                            else:
                                hours_ago = minutes_ago // 60
                                message_with_time['time_ago'] = f"{hours_ago} hours ago"
                                
                        elif timestamp.date() == (now - timedelta(days=1)).date():
                            message_with_time['time_display'] = "Yesterday"
                            message_with_time['time_ago'] = timestamp.strftime('%I:%M %p')
                        else:
                            message_with_time['time_display'] = timestamp.strftime('%d %b')
                            message_with_time['time_ago'] = timestamp.strftime('%Y')
                            
                    recent_messages.append(message_with_time)
                
                today = datetime.datetime.now().strftime('%Y-%m-%d')


                upcoming_appointments = []
                for appt in upcoming_appointments_raw:
                    processed_appt = process_appointment_for_calendar(appt)
                    upcoming_appointments.append(processed_appt)

                past_appointments = []
                for appt in past_appointments_raw:
                    processed_appt = process_appointment_for_calendar(appt)
                    past_appointments.append(processed_appt)

                serialized_upcoming = json.dumps(upcoming_appointments, default=serialize_datetime)
                therapist_data = await get_therapist_data(user["user_id"])

                return templates.TemplateResponse(
                    "dist/appointments/appointment_list.html",
                    {
                        "request": request,
                        "therapist": therapist_data,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "upcoming_appointments": upcoming_appointments,
                        "past_appointments": past_appointments,
                        "patients": patients,
                        "unread_messages_count": unread_messages_count,
                        "recent_messages": recent_messages,
                        "today": today,
                        "serialized_upcoming": serialized_upcoming  
                    }
                )
            except Exception as e:
                print(f"Database error in appointments page: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/front-page")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in appointments page: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")
        
    @app.get("/appointments/{appointment_id}")
    async def view_appointment(request: Request, appointment_id: int, user = Depends(get_current_user)):
        """Display the detailed view of an appointment"""
        session_id = request.cookies.get("session_id")
        therapist_data = await get_therapist_data(user["user_id"])

        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    "SELECT first_name, last_name FROM Therapists WHERE id = %s", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")

                cursor.execute(
                    """SELECT a.*, p.first_name as patient_first_name, p.last_name as patient_last_name,
                            p.diagnosis, p.phone, p.email
                    FROM Appointments a
                    JOIN Patients p ON a.patient_id = p.patient_id
                    WHERE a.appointment_id = %s AND a.therapist_id = %s""", 
                    (appointment_id, session_data["user_id"])
                )
                appointment = cursor.fetchone()
                
                if not appointment:
                    return RedirectResponse(url="/appointments?error=not_found")
                
                processed_appointment = process_appointment_for_calendar(appointment)
                
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                

                cursor.execute(
                    """SELECT m.message_id, m.subject, m.content, m.created_at, 
                            t.first_name, t.last_name, COALESCE(t.profile_image, 'avatar-1.jpg') as profile_image
                        FROM Messages m
                        JOIN Therapists t ON m.sender_id = t.id
                        WHERE m.recipient_id = %s AND m.is_read = FALSE
                        ORDER BY m.created_at DESC
                        LIMIT 4""",
                    (session_data["user_id"],)
                )
                messages_result = cursor.fetchall()

                recent_messages = []
                for message in messages_result:
                    message_with_time = message.copy()
                    
                    timestamp = message['created_at']
                    now = datetime.datetime.now()
                    if isinstance(timestamp, datetime.datetime):
                        diff = now - timestamp
                        if timestamp.date() == now.date():
                            message_with_time['time_display'] = timestamp.strftime('%I:%M %p')
                            
                            minutes_ago = diff.seconds // 60
                            if minutes_ago < 60:
                                message_with_time['time_ago'] = f"{minutes_ago} min ago"
                            else:
                                hours_ago = minutes_ago // 60
                                message_with_time['time_ago'] = f"{hours_ago} hours ago"
                                
                        elif timestamp.date() == (now - timedelta(days=1)).date():
                            message_with_time['time_display'] = "Yesterday"
                            message_with_time['time_ago'] = timestamp.strftime('%I:%M %p')
                        else:
                            message_with_time['time_display'] = timestamp.strftime('%d %b')
                            message_with_time['time_ago'] = timestamp.strftime('%Y')
                            
                    recent_messages.append(message_with_time)
                
                cursor.execute(
                    """SELECT plan_id, name, status 
                    FROM TreatmentPlans 
                    WHERE patient_id = %s 
                    ORDER BY start_date DESC""",
                    (appointment['patient_id'],)
                )
                treatment_plans = cursor.fetchall()
                
                return templates.TemplateResponse(
                    "dist/appointments/view_appointment.html",
                    {
                        "request": request,
                        "therapist": therapist_data,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "appointment": processed_appointment,
                        "treatment_plans": treatment_plans,
                        "unread_messages_count": unread_messages_count,
                        "recent_messages": recent_messages
                    }
                )
            except Exception as e:
                print(f"Database error in view appointment: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/appointments?error=database")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in view appointment: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")
            
    @app.get("/appointments/{appointment_id}/edit")
    async def edit_appointment_form(request: Request, appointment_id: int, user = Depends(get_current_user)):
        """Display the form to edit an appointment"""
        session_id = request.cookies.get("session_id")
        therapist_data = await get_therapist_data(user["user_id"])
        
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    "SELECT first_name, last_name FROM Therapists WHERE id = %s", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")
                
                cursor.execute(
                    """SELECT a.*, p.first_name as patient_first_name, p.last_name as patient_last_name 
                    FROM Appointments a
                    JOIN Patients p ON a.patient_id = p.patient_id
                    WHERE a.appointment_id = %s AND a.therapist_id = %s""", 
                    (appointment_id, session_data["user_id"])
                )
                appointment = cursor.fetchone()
                
                if not appointment:
                    return RedirectResponse(url="/appointments?error=not_found")
                
                processed_appointment = process_appointment_for_calendar(appointment)
                
                cursor.execute(
                    """SELECT patient_id, first_name, last_name, diagnosis, phone
                    FROM Patients 
                    WHERE therapist_id = %s
                    ORDER BY last_name, first_name""", 
                    (session_data["user_id"],)
                )
                patients = cursor.fetchall()
                
                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                
                cursor.execute(
                    """SELECT m.message_id, m.subject, m.content, m.created_at, 
                            t.first_name, t.last_name, COALESCE(t.profile_image, 'avatar-1.jpg') as profile_image
                        FROM Messages m
                        JOIN Therapists t ON m.sender_id = t.id
                        WHERE m.recipient_id = %s AND m.is_read = FALSE
                        ORDER BY m.created_at DESC
                        LIMIT 4""",
                    (session_data["user_id"],)
                )
                messages_result = cursor.fetchall()

                recent_messages = []
                for message in messages_result:
                    message_with_time = message.copy()
                    
                    timestamp = message['created_at']
                    now = datetime.datetime.now()
                    if isinstance(timestamp, datetime.datetime):
                        diff = now - timestamp
                        if timestamp.date() == now.date():
                            message_with_time['time_display'] = timestamp.strftime('%I:%M %p')
                            
                            minutes_ago = diff.seconds // 60
                            if minutes_ago < 60:
                                message_with_time['time_ago'] = f"{minutes_ago} min ago"
                            else:
                                hours_ago = minutes_ago // 60
                                message_with_time['time_ago'] = f"{hours_ago} hours ago"
                                
                        elif timestamp.date() == (now - timedelta(days=1)).date():
                            message_with_time['time_display'] = "Yesterday"
                            message_with_time['time_ago'] = timestamp.strftime('%I:%M %p')
                        else:
                            message_with_time['time_display'] = timestamp.strftime('%d %b')
                            message_with_time['time_ago'] = timestamp.strftime('%Y')
                            
                    recent_messages.append(message_with_time)
                
                appointment_date = appointment['appointment_date']
                formatted_date = appointment_date.strftime('%Y-%m-%d') if isinstance(appointment_date, datetime.date) else appointment_date
                
                appointment_time = appointment['appointment_time']
                if isinstance(appointment_time, datetime.time):
                    formatted_time = appointment_time.strftime('%H:%M')
                elif isinstance(appointment_time, datetime.timedelta):
                    total_seconds = appointment_time.total_seconds()
                    hours = int(total_seconds // 3600)
                    minutes = int((total_seconds % 3600) // 60)
                    formatted_time = f"{hours:02d}:{minutes:02d}"
                else:
                    formatted_time = appointment_time
                    
                status_options = ['Scheduled', 'Completed', 'Cancelled', 'No-Show']
                
                return templates.TemplateResponse(
                    "dist/appointments/edit_appointment.html",
                    {
                        "request": request,
                        "therapist": therapist_data,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "appointment": processed_appointment,
                        "appointment_date": formatted_date,
                        "appointment_time": formatted_time,
                        "patients": patients,
                        "unread_messages_count": unread_messages_count,
                        "recent_messages": recent_messages,
                        "status_options": status_options
                    }
                )
            except Exception as e:
                print(f"Database error in edit appointment form: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/appointments?error=database")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in edit appointment form: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")
        
    @app.get("/appointments/{appointment_id}/delete")
    async def delete_appointment(request: Request, appointment_id: int, user=Depends(get_current_user)):
        """Delete an appointment"""
        session_id = request.cookies.get("session_id")
        
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    """SELECT appointment_id 
                    FROM Appointments 
                    WHERE appointment_id = %s AND therapist_id = %s""",
                    (appointment_id, session_data["user_id"])
                )
                appointment = cursor.fetchone()
                
                if not appointment:
                    return RedirectResponse(url="/appointments?error=not_found")
                
                cursor.execute(
                    "DELETE FROM Appointments WHERE appointment_id = %s",
                    (appointment_id,)
                )
                db.commit()
                
                return RedirectResponse(url="/appointments?success=deleted", status_code=303)
            
            except Exception as e:
                print(f"Database error in delete appointment: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/appointments?error=database")
            
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        
        except Exception as e:
            print(f"Error in delete appointment: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")


    @app.post("/appointments/{appointment_id}/edit")
    async def update_appointment(request: Request, appointment_id: int):
        """Handle appointment update form submission"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            form_data = await request.form()
            
            patient_id = form_data.get("patient_id")
            appointment_date = form_data.get("appointment_date")
            appointment_time = form_data.get("appointment_time")
            duration = form_data.get("duration", "60")
            notes = form_data.get("notes")
            status = form_data.get("status", "Scheduled")
            
            if not patient_id or not appointment_date or not appointment_time:
                return RedirectResponse(
                    url=f"/appointments/{appointment_id}/edit?error=missing_fields", 
                    status_code=303
                )

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor()
                

                cursor.execute(
                    """SELECT appointment_id 
                    FROM Appointments 
                    WHERE appointment_id = %s AND therapist_id = %s""",
                    (appointment_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    print(f"Appointment {appointment_id} does not belong to therapist {session_data['user_id']}")
                    return RedirectResponse(url="/appointments?error=unauthorized")

                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE patient_id = %s AND therapist_id = %s",
                    (patient_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    print(f"Patient {patient_id} does not belong to therapist {session_data['user_id']}")
                    return RedirectResponse(url=f"/appointments/{appointment_id}/edit?error=invalid_patient")
                
                try:

                    try:
                        time_obj = datetime.datetime.strptime(appointment_time, "%H:%M").time()
                    except ValueError:
                        try:
                            time_obj = datetime.datetime.strptime(appointment_time, "%I:%M %p").time()
                        except ValueError:
                            time_obj = datetime.datetime.strptime(appointment_time, "%I:%M%p").time()

                    cursor.execute(
                        """UPDATE Appointments 
                        SET patient_id = %s, 
                            appointment_date = %s, 
                            appointment_time = %s, 
                            duration = %s, 
                            notes = %s, 
                            status = %s,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE appointment_id = %s""",
                        (patient_id, appointment_date, time_obj, duration, notes, status, appointment_id)
                    )
                    db.commit()
                    
                    return RedirectResponse(url="/appointments?success=updated", status_code=303)
                except ValueError as ve:
                    print(f"Time parsing error: {ve}")
                    return RedirectResponse(url=f"/appointments/{appointment_id}/edit?error=invalid_time_format")
                    
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error updating appointment: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url=f"/appointments/{appointment_id}/edit?error=db_error")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error updating appointment: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")
    
    @app.post("/appointments/new")
    async def create_appointment(request: Request, user=Depends(get_current_user)):
        """Handle appointment creation"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            form_data = await request.form()
            
            patient_id = form_data.get("patient_id")
            appointment_date = form_data.get("appointment_date")
            appointment_time = form_data.get("appointment_time")
            duration = form_data.get("duration", "60")
            notes = form_data.get("notes")
            
            if not patient_id or not appointment_date or not appointment_time:
                return RedirectResponse(url="/appointments/new?error=missing_fields", status_code=303)

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor()
                
                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE patient_id = %s AND therapist_id = %s",
                    (patient_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    print(f"Patient {patient_id} does not belong to therapist {session_data['user_id']}")
                    return RedirectResponse(url="/appointments/new?error=invalid_patient")
                
                try:
                    try:
                        time_obj = datetime.datetime.strptime(appointment_time, "%H:%M").time()
                    except ValueError:
                        try:
                            time_obj = datetime.datetime.strptime(appointment_time, "%I:%M %p").time()
                        except ValueError:
                            time_obj = datetime.datetime.strptime(appointment_time, "%I:%M%p").time()
                    
                    cursor.execute(
                        """INSERT INTO Appointments 
                        (patient_id, therapist_id, appointment_date, appointment_time, duration, notes, status) 
                        VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                        (patient_id, session_data["user_id"], appointment_date, time_obj, duration, notes, "Scheduled")
                    )
                    db.commit()
                    
                    return RedirectResponse(url="/appointments", status_code=303)
                except ValueError as ve:
                    print(f"Time parsing error: {ve}")
                    return RedirectResponse(url="/appointments/new?error=invalid_time_format")
                    
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error creating appointment: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/appointments/new?error=db_error")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error creating appointment: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")

    @app.post("/appointments/{appointment_id}/status")
    async def update_appointment_status(
        request: Request,
        appointment_id: int,
        status: str = Form(...),
        session_notes: str = Form(None)
    ):
        """Route to update appointment status"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(status_code=401, content={"success": False, "message": "Not authenticated"})

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"success": False, "message": "Not authenticated"})

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor()
                
                cursor.execute(
                    """SELECT appointment_id FROM Appointments 
                    WHERE appointment_id = %s AND therapist_id = %s""",
                    (appointment_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    return JSONResponse(
                        status_code=403, 
                        content={"success": False, "message": "You don't have permission to update this appointment"}
                    )
                
                notes_update = ""
                if session_notes:
                    notes_update = f", notes = CONCAT(COALESCE(notes, ''), '\n\n{session_notes}')"
                
                cursor.execute(
                    f"UPDATE Appointments SET status = %s{notes_update} WHERE appointment_id = %s",
                    (status, appointment_id)
                )
                
                db.commit()
                
                return JSONResponse(content={"success": True, "message": f"Appointment marked as {status}"})
                
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error in update appointment status: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500, 
                    content={"success": False, "message": f"Error updating appointment: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in update appointment status: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500, 
                content={"success": False, "message": "Server error"}
            )

    @app.get("/exercises")
    async def exercises_page(request: Request, user=Depends(get_current_user)):
        db = get_Mysql_db()
        cursor = db.cursor(dictionary=True)

        try:
            cursor.execute(
                """SELECT e.*, c.name as category_name 
                FROM Exercises e
                LEFT JOIN ExerciseCategories c ON e.category_id = c.category_id
                """
            )
            exercises = cursor.fetchall()

            cursor.execute("SELECT * FROM ExerciseCategories")
            categories = cursor.fetchall()
            
            cursor.execute("SELECT * FROM TreatmentPlans")
            treatment_plans = cursor.fetchall()
            
            therapist_data = await get_therapist_data(user["user_id"])

            return templates.TemplateResponse(
                "dist/exercises/exercise_list.html", 
                {
                    "request": request,
                    "treatment_plans": treatment_plans,
                    "exercises": exercises,
                    "categories": categories,
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"]
                }
            )
        finally:
            cursor.close()
            db.close()

    @app.get("/exercises/add")
    async def add_exercise_page(request: Request, user=Depends(get_current_user)):
        db = get_Mysql_db()
        cursor = db.cursor(dictionary=True)

        try:
            cursor.execute("SELECT * FROM ExerciseCategories")
            categories = cursor.fetchall()

            therapist_data = await get_therapist_data(user["user_id"])

            return templates.TemplateResponse(
                "dist/exercises/add_exercise.html", 
                {
                    "request": request,
                    "categories": categories,
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"]
                }
            )
        finally:
            cursor.close()
            db.close()

    @app.post("/exercises/add")
    async def add_exercise(
        request: Request,
        name: str = Form(...),
        category_id: int = Form(...),
        description: str = Form(None),
        video_url: str = Form(None),
        duration: int = Form(None),
        difficulty: str = Form(None),
        instructions: str = Form(None),
        user=Depends(get_current_user)
    ):
        db = get_Mysql_db()
        cursor = db.cursor()

        try:
            cursor.execute(
                """INSERT INTO Exercises 
                (therapist_id, category_id, name, description, video_url, duration, difficulty, instructions) 
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)""",
                (user["user_id"], category_id, name, description, video_url, duration, difficulty, instructions)
            )
            db.commit()
            return RedirectResponse(url="/exercises", status_code=303)
        except Exception as e:
            print(f"Error adding exercise: {e}")
            return RedirectResponse(url="/exercises/add", status_code=303)
        finally:
            cursor.close()
            db.close()

    @app.get("/treatment-plans")
    async def treatment_plans_page(request: Request, user=Depends(get_current_user)):
        db = get_Mysql_db()
        cursor = db.cursor(dictionary=True)

        try:
            cursor.execute(
                """SELECT tp.*, p.first_name, p.last_name 
                FROM TreatmentPlans tp
                JOIN Patients p ON tp.patient_id = p.patient_id
                WHERE tp.therapist_id = %s
                ORDER BY tp.created_at DESC""", 
                (user["user_id"],)
            )
            treatment_plans = cursor.fetchall()

            therapist_data = await get_therapist_data(user["user_id"])

            return templates.TemplateResponse(
                "dist/treatment_plans/plan_list.html", 
                {
                    "request": request,
                    "treatment_plans": treatment_plans,
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"]
                }
            )
        finally:
            cursor.close()
            db.close()
    @app.post("/treatment-plans/delete")
    async def delete_treatment_plan(request: Request, user=Depends(get_current_user)):
        form_data = await request.form()
        plan_id = form_data.get("plan_id")
        
        if not plan_id:
            return RedirectResponse(url="/treatment-plans", status_code=303)
        
        db = get_Mysql_db()
        cursor = db.cursor()
        try:
            cursor.execute(
                "SELECT therapist_id FROM TreatmentPlans WHERE plan_id = %s",
                (plan_id,)
            )
            result = cursor.fetchone()
            
            if not result or result[0] != user["user_id"]:
                return RedirectResponse(url="/treatment-plans", status_code=303)
            
            cursor.execute(
                "DELETE FROM TreatmentPlans WHERE plan_id = %s AND therapist_id = %s",
                (plan_id, user["user_id"])
            )
            db.commit()
            
            return RedirectResponse(url="/treatment-plans", status_code=303)
        except Exception as e:
            print(f"Error deleting treatment plan: {e}")
            return RedirectResponse(url="/treatment-plans", status_code=303)
        finally:
            cursor.close()
            db.close()
    
    @app.post("/treatment-plans/new")
    async def create_treatment_plan(request: Request):
        """Route to handle creating a new treatment plan with exercises"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")
        
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            

            form = await request.form()

            patient_id = form.get("patient_id")
            plan_name = form.get("plan_name")
            description = form.get("description", "")
            start_date = form.get("start_date")
            end_date = form.get("end_date")
            status = form.get("status", "Active")
            
            print(f"Plan details: patient={patient_id}, name={plan_name}, start={start_date}, end={end_date}, status={status}")
            

            if not patient_id or not plan_name or not start_date:
                error_msg = "Missing required fields: "
                if not patient_id: error_msg += "patient, "
                if not plan_name: error_msg += "plan name, "
                if not start_date: error_msg += "start date"
                
                print(f"ERROR: {error_msg}")
                

                db = get_Mysql_db()
                cursor = db.cursor(dictionary=True)
                cursor.execute("SELECT patient_id, first_name, last_name FROM Patients WHERE therapist_id = %s", 
                            (session_data["user_id"],))
                patients = cursor.fetchall()
                cursor.execute("SELECT * FROM Exercises")
                exercises = cursor.fetchall()
                cursor.close()
                db.close()
                
                therapist_data = await get_therapist_data(session_data["user_id"])
                
                return templates.TemplateResponse(
                    "dist/treatment_plans/new_plan.html", 
                    {
                        "request": request,
                        "error": error_msg,
                        "patients": patients,
                        "exercises": exercises,
                        "therapist":therapist_data,
                        "first_name": therapist_data["first_name"],
                        "last_name": therapist_data["last_name"]
                    }
                )
            
            exercises = form.getlist("exercises[]")
            sets = form.getlist("sets[]")
            repetitions = form.getlist("repetitions[]")
            frequencies = form.getlist("frequency[]")
            durations = form.getlist("duration[]")
            exercise_notes = form.getlist("exercise_notes[]")

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor()

                cursor.execute(
                    """INSERT INTO TreatmentPlans 
                    (patient_id, therapist_id, name, description, start_date, end_date, status) 
                    VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                    (patient_id, session_data["user_id"], plan_name, description, start_date, end_date, status)
                )
                plan_id = cursor.lastrowid
                print(f"Created plan with ID: {plan_id}")
                

                for i in range(len(exercises)):
                    exercise_id = exercises[i] if i < len(exercises) else None
                    
                    if not exercise_id or exercise_id == "":
                        print(f"Skipping empty exercise at index {i}")
                        continue
                        
                    exercise_sets = sets[i] if i < len(sets) and sets[i] else None
                    exercise_reps = repetitions[i] if i < len(repetitions) and repetitions[i] else None
                    exercise_freq = frequencies[i] if i < len(frequencies) and frequencies[i] else None
                    exercise_duration = durations[i] if i < len(durations) and durations[i] else None
                    exercise_note = exercise_notes[i] if i < len(exercise_notes) else None
                    
                    print(f"Adding exercise: ID={exercise_id}, Sets={exercise_sets}, Reps={exercise_reps}, Freq={exercise_freq}, Duration={exercise_duration}")
                    
                    try:
                        cursor.execute(
                            """INSERT INTO TreatmentPlanExercises
                            (plan_id, exercise_id, sets, repetitions, frequency, duration, notes)
                            VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                            (plan_id, exercise_id, exercise_sets, exercise_reps, exercise_freq, exercise_duration, exercise_note)
                        )
                        print(f"Successfully added exercise {exercise_id} to plan {plan_id}")
                    except Exception as ex:
                        print(f"Error adding exercise {exercise_id}: {ex}")

                db.commit()
                print(f"Treatment plan {plan_id} created successfully with exercises")
                return RedirectResponse(url="/treatment-plans", status_code=303)
                
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                

                cursor = db.cursor(dictionary=True)
                cursor.execute("SELECT patient_id, first_name, last_name FROM Patients WHERE therapist_id = %s", 
                            (session_data["user_id"],))
                patients = cursor.fetchall()
                cursor.execute("SELECT * FROM Exercises")
                exercises = cursor.fetchall()
                
                therapist_data = await get_therapist_data(session_data["user_id"])
                
                return templates.TemplateResponse(
                    "dist/treatment_plans/new_plan.html", 
                    {
                        "request": request,
                        "error": f"Database error: {str(e)}",
                        "patients": patients,
                        "exercises": exercises,
                        "therapist": therapist_data,
                        "first_name": therapist_data["first_name"],
                        "last_name": therapist_data["last_name"]
                    }
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
                
        except Exception as e:
            print(f"General error: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/front-page")
            
    @app.get("/therapists")
    async def get_therapists():
        """API endpoint to get a list of all therapists for the mobile app"""
        try:
            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:
                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image, 
                            specialties, address, rating, review_count, 
                            is_accepting_new_patients
                    FROM Therapists 
                    WHERE is_accepting_new_patients = TRUE
                    ORDER BY rating DESC, review_count DESC"""
                )
                therapists = cursor.fetchall()

                base_url = app.state.base_url
                static_dir = getattr(app.state, 'static_directory', "/PERCEPTRONX/Frontend_Web/static")
                
                print(f"Using base URL: {base_url}")
                
                formatted_therapists = []
                for therapist in therapists:
                    specialties = safely_parse_json_field(therapist['specialties'], [])
                    
                    profile_image = therapist['profile_image']
                    matched_image = find_best_matching_image(
                        therapist["id"], 
                        profile_image, 
                        static_dir
                    )
                    
                    photoUrl = f"/static/assets/images/user/{matched_image}"
                    
                    print(f"Therapist ID: {therapist['id']}, Original Image: {profile_image}, Matched: {matched_image}, URL: {photoUrl}")

                    formatted_therapists.append({
                        "id": therapist["id"],
                        "name": f"{therapist['first_name']} {therapist['last_name']}",
                        "photoUrl": photoUrl,
                        "specialties": specialties,
                        "location": therapist["address"] or "Location not provided",
                        "rating": float(therapist["rating"] or 0),
                        "reviewCount": therapist["review_count"] or 0,
                        "distance": 0.0, 
                        "nextAvailable": "Today" 
                    })

                return formatted_therapists

            except Exception as e:
                print(f"Database error in get therapists API: {e}")
                return JSONResponse(
                    status_code=500,
                    content={"error": f"Internal server error: {str(e)}"}
                )
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in get therapists API: {e}")
            return JSONResponse(
                status_code=500,
                content={"error": f"Server error: {str(e)}"}
            )
            
    @app.get("/therapists/{id}")
    async def get_therapist_details(id: int):
        """API endpoint to get detailed information about a specific therapist"""
        try:
            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)
            try:
                print(f"Looking up therapist with ID: {id}")
                
                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image,
                            bio, experience_years, specialties, education, languages,
                            address, rating, review_count,
                            is_accepting_new_patients, average_session_length
                    FROM Therapists
                    WHERE id = %s""",
                    (id,)
                )
                therapist = cursor.fetchone()
                if not therapist:
                    print(f"No therapist found with ID: {id}")
                    return JSONResponse(
                        status_code=404,
                        content={"error": "Therapist not found"}
                    )
                
                if therapist['first_name'] is None:
                    therapist['first_name'] = ""
                if therapist['last_name'] is None:
                    therapist['last_name'] = ""
                    
                print(f"Therapist found: {therapist['first_name']} {therapist['last_name']}")
                
                for field in ['specialties', 'education', 'languages']:
                    therapist[field] = safely_parse_json_field(therapist[field], [])
                
                static_dir = getattr(app.state, 'static_directory', "/PERCEPTRONX/Frontend_Web/static")
                profile_image = therapist['profile_image']
                matched_image = find_best_matching_image(id, profile_image, static_dir)
                photoUrl = f"/static/assets/images/user/{matched_image}"
                print(f"Therapist detail ID: {id}, Original Image: {profile_image}, Matched: {matched_image}, URL: {photoUrl}")
                
                formatted_therapist = {
                    "id": therapist["id"],
                    "first_name": therapist["first_name"],
                    "last_name": therapist["last_name"],
                    "name": f"{therapist['first_name']} {therapist['last_name']}",
                    "photoUrl": photoUrl,
                    "profile_image": matched_image,
                    "specialties": therapist["specialties"],
                    "bio": therapist["bio"] or "",
                    "experienceYears": therapist["experience_years"] or 0,
                    "experience_years": therapist["experience_years"] or 0,
                    "education": therapist["education"],
                    "languages": therapist["languages"],
                    "address": therapist["address"] or "",
                    "rating": float(therapist["rating"] or 0),
                    "reviewCount": therapist["review_count"] or 0,
                    "review_count": therapist["review_count"] or 0,
                    "isAcceptingNewPatients": bool(therapist["is_accepting_new_patients"]),
                    "is_accepting_new_patients": bool(therapist["is_accepting_new_patients"]),
                    "averageSessionLength": therapist["average_session_length"] or 60,
                    "average_session_length": therapist["average_session_length"] or 60
                }
                
                print(f"Formatted therapist: {formatted_therapist}")
                return formatted_therapist
            except Exception as e:
                print(f"Database error in get therapist details API: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"error": f"Internal server error: {str(e)}"}
                )
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in get therapist details API: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"error": f"Server error: {str(e)}"}
            )

    def convert_mysql_time_to_time(mysql_time):
        """Convert MySQL TIME (timedelta) to datetime.time"""
        if isinstance(mysql_time, datetime.timedelta):
            return (datetime.datetime.min + mysql_time).time()
        return mysql_time

    @app.get("/therapists/{id}/availability")
    async def get_therapist_availability(id: int, date: str = None):
        """API endpoint to get available time slots for a therapist"""
        try:
            if not date:
                date = datetime.datetime.now().strftime("%Y-%m-%d")
            
            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:
                cursor.execute(
                    "SELECT id, average_session_length FROM Therapists WHERE id = %s",
                    (id,)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return JSONResponse(
                        status_code=404,
                        content={"error": "Therapist not found"}
                    )
                
                cursor.execute(
                    """SELECT appointment_time, duration 
                    FROM Appointments 
                    WHERE therapist_id = %s AND appointment_date = %s 
                    AND status != 'Cancelled'""",
                    (id, date)
                )
                booked_slots = cursor.fetchall()
                
                start_hour = 9
                end_hour = 17
                
                slot_duration = therapist['average_session_length'] or 60
                
                available_slots = []
                current_time = datetime.time(start_hour, 0)
                end_time = datetime.time(end_hour, 0)
                
                slot_id = 1
                while current_time < end_time:
                    slot_end = (datetime.datetime.combine(datetime.date.today(), current_time) + 
                                datetime.timedelta(minutes=slot_duration)).time()
                    
                    is_available = True
                    for booked in booked_slots:
                        booked_start = convert_mysql_time_to_time(booked['appointment_time'])
                        booked_end_dt = (datetime.datetime.combine(datetime.date.today(), booked_start) + 
                                        datetime.timedelta(minutes=booked['duration']))
                        booked_end = booked_end_dt.time()
                        
                        if (current_time < booked_end and slot_end > booked_start):
                            is_available = False
                            break
                    
                    formatted_time = current_time.strftime("%I:%M %p")
                    
                    available_slots.append({
                        "id": slot_id,
                        "date": date,
                        "time": formatted_time,
                        "isAvailable": is_available
                    })
                    
                    slot_id += 1
                    
                    current_time_dt = datetime.datetime.combine(datetime.date.today(), current_time)
                    current_time_dt += datetime.timedelta(minutes=30)
                    current_time = current_time_dt.time()
                
                return available_slots

            except Exception as e:
                print(f"Database error in get therapist availability API: {e}")
                return JSONResponse(
                    status_code=500,
                    content={"error": f"Internal server error: {str(e)}"}
                )
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in get therapist availability API: {e}")
            return JSONResponse(        
                status_code=500,
                content={"error": f"Server error: {str(e)}"}
            )

    @app.post("/api/book-appointment")
    async def book_appointment(appointment_request: AppointmentRequest, request: Request):
        """API endpoint to request an appointment with a therapist"""
        session_id = request.cookies.get("session_id")
        print(f"Appointment request - Cookie session ID: {session_id}")

        db = get_Mysql_db()
        cursor = db.cursor()

        try:
            cursor.execute(
                "SELECT id FROM Therapists WHERE id = %s",
                (appointment_request.therapist_id,)
            )
            therapist = cursor.fetchone()

            if not therapist:
                return JSONResponse(
                    status_code=404,
                    content={"status": "failed", "message": "Therapist not found"}
                )

            user_info = None
            user_id = None

            if session_id:
                try:
                    session_data = await get_session_data(session_id)
                    if session_data and hasattr(session_data, 'user_id'):
                        user_id = session_data.user_id
                        cursor.execute(
                            "SELECT username, email, user_id FROM users WHERE user_id = %s",
                            (user_id,)
                        )
                        user_info = cursor.fetchone()
                except Exception as e:
                    print(f"Error getting session data: {e}")

            patient_id = None

            if user_info:
                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE email = %s",
                    (user_info[1],)
                )
                patient_record = cursor.fetchone()
                print(f"patient_record: {patient_record}")

                if patient_record:
                    patient_id = patient_record[0]
                else:
                    cursor.execute(
                        """INSERT INTO Patients 
                        (therapist_id, first_name, last_name, email, user_id) 
                        VALUES (%s, %s, %s, %s, %s)""",
                        (appointment_request.therapist_id, user_info[0], "", user_info[1], user_info[2])
                    )
                    db.commit()
                    patient_id = cursor.lastrowid
            else:
                cursor.execute(
                    """INSERT INTO Patients 
                    (therapist_id, first_name, last_name, email) 
                    VALUES (%s, %s, %s, %s)""",
                    (appointment_request.therapist_id, "Guest", "User", f"guest_{int(time.time())}@example.com")
                )
                db.commit()
                patient_id = cursor.lastrowid

            time_parts = appointment_request.time.split()
            time_str = time_parts[0]
            am_pm = time_parts[1] if len(time_parts) > 1 else "AM"

            try:
                time_obj = datetime.datetime.strptime(f"{time_str} {am_pm}", "%I:%M %p").time()
            except ValueError:
                try:
                    time_obj = datetime.datetime.strptime(time_str, "%H:%M").time()
                except ValueError:
                    return JSONResponse(
                        status_code=400,
                        content={"status": "failed", "message": "Invalid time format"}
                    )

            duration = 60

            full_notes = f"Type: {appointment_request.type}\n"
            if appointment_request.notes:
                full_notes += f"Notes: {appointment_request.notes}\n"
            if appointment_request.insuranceProvider:
                full_notes += f"Insurance: {appointment_request.insuranceProvider}\n"
            if appointment_request.insuranceMemberId:
                full_notes += f"Member ID: {appointment_request.insuranceMemberId}"

            cursor.execute(
                """INSERT INTO Appointments 
                (patient_id, therapist_id, appointment_date, appointment_time, duration, notes, status) 
                VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                (patient_id, appointment_request.therapist_id, appointment_request.date,
                time_obj, duration, full_notes, "Scheduled")
            )
            db.commit()

            return {"status": "success", "message": "Appointment scheduled successfully"}

        except Exception as e:
            db.rollback()
            print(f"Database error in request appointment API: {e}")
            return JSONResponse(
                status_code=500,
                content={"status": "failed", "message": f"Error requesting appointment: {str(e)}"}
            )
        finally:
            cursor.close()
            db.close()

    @app.post("/therapists/{id}/add_patient")
    async def add_patient_to_therapist(request: Request, id: int, patient: dict):
        """API endpoint to add a user as a patient to a therapist"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"status": "invalid", "detail": "Not authenticated"}
            )

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return JSONResponse(
                    status_code=401,
                    content={"status": "invalid", "detail": "Not authenticated"}
                )

            user_id = session_data["user_id"]
            
            db = get_Mysql_db()
            cursor = db.cursor()

            try:
 
                cursor.execute(
                    "SELECT id FROM Therapists WHERE id = %s",
                    (id,)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return JSONResponse(
                        status_code=404,
                        content={"status": "invalid", "detail": "Therapist not found"}
                    )
                
 
                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                existing_patient = cursor.fetchone()
                
                if existing_patient:
 
                    cursor.execute(
                        """UPDATE Patients 
                        SET therapist_id = %s,
                            first_name = %s,
                            last_name = %s,
                            phone = %s,
                            diagnosis = %s,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = %s""",
                        (id, patient.get('first_name', ''), patient.get('last_name', ''), 
                        patient.get('phone', ''), patient.get('diagnosis', ''), user_id)
                    )
                else:
 
                    cursor.execute(
                        "SELECT email FROM users WHERE user_id = %s",
                        (user_id,)
                    )
                    user_email = cursor.fetchone()
                    email = user_email[0] if user_email else ''
                    
 
                    cursor.execute(
                        """INSERT INTO Patients 
                        (therapist_id, user_id, first_name, last_name, email, phone, diagnosis) 
                        VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                        (id, user_id, patient.get('first_name', ''), patient.get('last_name', ''), 
                        email, patient.get('phone', ''), patient.get('diagnosis', ''))
                    )
                
                db.commit()
                
                return {"status": "valid", "message": "Patient added successfully"}

            except Exception as e:
                db.rollback()
                print(f"Database error in add patient API: {e}")
                return JSONResponse(
                    status_code=500,
                    content={"status": "invalid", "detail": f"Error adding patient: {str(e)}"}
                )
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in add patient API: {e}")
            return JSONResponse(
                status_code=500,
                content={"status": "invalid", "detail": f"Server error: {str(e)}"}
            )

    @app.get("/api/user/therapist")
    async def get_user_therapist_data(request: Request):
        """API endpoint to get the therapist data for the current logged-in user in the format matching the Kotlin Therapist class"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )

        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(
                    status_code=401,
                    content={"detail": "Not authenticated"}
                )

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    """SELECT p.therapist_id, t.* 
                    FROM Patients p
                    JOIN Therapists t ON p.therapist_id = t.id
                    WHERE p.user_id = %s""",
                    (user_id,)
                )
                result = cursor.fetchone()
                
                if not result:
                    return JSONResponse(
                        status_code=404,
                        content={"detail": "Therapist not found for this user"}
                    )
                
                for field in ['specialties', 'education', 'languages']:
                    result[field] = safely_parse_json_field(result[field], [])
                
                formatted_therapist = {
                    "id": result["id"],
                    "first_name": result["first_name"] or "",
                    "last_name": result["last_name"] or "",
                    "company_email": result["company_email"] or "",
                    "profile_image": result["profile_image"] or "",
                    "bio": result["bio"] or "",
                    "experience_years": result["experience_years"] or 0,
                    "specialties": result["specialties"],
                    "education": result["education"],
                    "languages": result["languages"],
                    "address": result["address"] or "",
                    "rating": float(result["rating"] or 0),
                    "review_count": result["review_count"] or 0,
                    "is_accepting_new_patients": bool(result["is_accepting_new_patients"]),
                    "average_session_length": result["average_session_length"] or 60
                }
                
                return formatted_therapist

            except Exception as e:
                print(f"Database error in get user therapist data API: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Internal server error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get user therapist data API: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
            
    def format_mysql_time(mysql_time):
        """Converts MySQL TIME (timedelta) to formatted string like '02:30 PM'"""
        if isinstance(mysql_time, timedelta):
            mysql_time = (datetime.datetime.min + mysql_time).time()
        return mysql_time.strftime("%I:%M %p") if mysql_time else "N/A"


    @app.get("/api/user/appointments")
    async def get_user_appointments_data(request: Request):
        """API endpoint to get appointments for the current logged-in user"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )

        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(
                    status_code=401,
                    content={"detail": "Not authenticated"}
                )

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient_record = cursor.fetchone()
                
                if not patient_record:
                    return []
                
                patient_id = patient_record['patient_id']
                
                cursor.execute(
                    """SELECT a.* 
                    FROM Appointments a
                    WHERE a.patient_id = %s
                    ORDER BY a.appointment_date DESC, a.appointment_time DESC""",
                    (patient_id,)
                )
                appointments = cursor.fetchall()
                
                formatted_appointments = []
                for appointment in appointments:
                    notes_info = {"appointmentType": "", "additionalNotes": "", "insurance": "", "memberId": 0}
                    if appointment["notes"]:
                        lines = appointment["notes"].split('\n')
                        for line in lines:
                            if line.startswith('Type:'):
                                notes_info["appointmentType"] = line[5:].strip()
                            elif line.startswith('Notes:'):
                                notes_info["additionalNotes"] = line[6:].strip()
                            elif line.startswith('Insurance:'):
                                notes_info["insurance"] = line[10:].strip()
                            elif line.startswith('Member ID:'):
                                id_str = line[10:].strip()
                                try:
                                    notes_info["memberId"] = int(id_str)
                                except ValueError:
                                    notes_info["memberId"] = 0
                    
                    formatted_appointment = {
                        "appointment_id": appointment["appointment_id"],
                        "patient_id": appointment["patient_id"],
                        "therapist_id": appointment["therapist_id"],
                        "appointment_date": appointment["appointment_date"].isoformat() if appointment["appointment_date"] else "",
                        "appointment_time": format_mysql_time(appointment["appointment_time"]),
                        "duration": appointment["duration"] or 60,
                        "status": appointment["status"] or "Scheduled",
                        "notes": appointment["notes"] or "",
                        "appointmentType": notes_info["appointmentType"],
                        "additionalNotes": notes_info["additionalNotes"],  
                        "insurance": notes_info["insurance"],             
                        "memberId": notes_info["memberId"],               
                        "created_at": appointment["created_at"].isoformat() if appointment["created_at"] else "",
                        "updated_at": appointment["updated_at"].isoformat() if appointment["updated_at"] else ""
                    }
                    
                    formatted_appointments.append(formatted_appointment)
                
                return formatted_appointments

            except Exception as e:
                print(f"Database error in get user appointments data API: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Internal server error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get user appointments data API: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
            
    @app.get("/api/user/treatment-plans")
    async def get_user_treatment_plans(request: Request):
        """API endpoint to get treatment plans for the current logged-in user"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT patient_id, therapist_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(status_code=404, content={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                therapist_id = patient["therapist_id"]
                

                cursor.execute(
                    "SELECT first_name, last_name FROM Therapists WHERE id = %s",
                    (therapist_id,)
                )
                therapist = cursor.fetchone()
                therapist_name = f"{therapist['first_name']} {therapist['last_name']}" if therapist else "Unknown"
                

                cursor.execute(
                    """
                    SELECT * FROM TreatmentPlans 
                    WHERE patient_id = %s
                    ORDER BY created_at DESC
                    """,
                    (patient_id,)
                )
                treatment_plans_raw = cursor.fetchall()
                
                treatment_plans = []
                for plan in treatment_plans_raw:

                    cursor.execute(
                        """
                        SELECT 
                            COUNT(*) as total_exercises,
                            SUM(CASE WHEN pep.progress_id IS NOT NULL THEN 1 ELSE 0 END) as completed_exercises
                        FROM TreatmentPlanExercises tpe
                        LEFT JOIN PatientExerciseProgress pep ON 
                            tpe.plan_exercise_id = pep.plan_exercise_id AND
                            pep.patient_id = %s
                        WHERE tpe.plan_id = %s
                        """,
                        (patient_id, plan["plan_id"])
                    )
                    progress_data = cursor.fetchone()
                    
                    total_exercises = progress_data["total_exercises"] or 0
                    completed_exercises = progress_data["completed_exercises"] or 0
                    progress = completed_exercises / total_exercises if total_exercises > 0 else 0
                    

                    cursor.execute(
                        """
                        SELECT tpe.*, e.name, e.description, e.video_url, e.video_type, 
                            e.duration, e.instructions, e.video_filename as thumbnailUrl,
                            (pep.progress_id IS NOT NULL) as completed
                        FROM TreatmentPlanExercises tpe
                        JOIN Exercises e ON tpe.exercise_id = e.exercise_id
                        LEFT JOIN PatientExerciseProgress pep ON 
                            tpe.plan_exercise_id = pep.plan_exercise_id AND
                            pep.patient_id = %s
                        WHERE tpe.plan_id = %s
                        """,
                        (patient_id, plan["plan_id"])
                    )
                    exercises_raw = cursor.fetchall()
                    
                    exercises = []
                    for ex in exercises_raw:

                        exercise = {
                            "exerciseId": ex["exercise_id"],
                            "planExerciseId": ex["plan_exercise_id"],
                            "name": ex["name"],
                            "description": ex["description"] or ex["instructions"] or "",
                            "videoUrl": ex["video_url"],
                            "imageUrl": None,  
                            "videoType": ex["video_type"] or "",
                            "sets": ex["sets"] or 3,
                            "repetitions": ex["repetitions"] or 10,
                            "frequency": ex["frequency"] or "Daily",
                            "duration": ex["duration"],
                            "completed": bool(ex["completed"]),
                            "thumbnailUrl": ex["thumbnailUrl"]
                        }
                        exercises.append(exercise)
                    

                    formatted_plan = {
                        "planId": plan["plan_id"],
                        "patientId": plan["patient_id"],
                        "therapistId": plan["therapist_id"],
                        "name": plan["name"],
                        "description": plan["description"],
                        "startDate": plan["start_date"].isoformat() if plan["start_date"] else None,
                        "endDate": plan["end_date"].isoformat() if plan["end_date"] else None,
                        "status": plan["status"],
                        "createdAt": plan["created_at"].isoformat() if plan["created_at"] else None,
                        "updatedAt": plan["updated_at"].isoformat() if plan["updated_at"] else None,
                        "therapistName": therapist_name,
                        "progress": progress,
                        "exercises": exercises
                    }
                    
                    treatment_plans.append(formatted_plan)
                
                return treatment_plans
                
            except Exception as e:
                print(f"Database error in get user treatment plans: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get user treatment plans: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )

    @app.get("/api/user/exercises/progress")
    async def get_user_exercises_progress(request: Request):
        """API endpoint to get overall user progress across all exercises and treatment plans"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            print(f"Getting progress for user_id: {user_id}")
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(status_code=404, content={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                print(f"Found patient_id: {patient_id}")
                

                cursor.execute(
                    """
                    SELECT plan_id, status FROM TreatmentPlans WHERE patient_id = %s
                    """,
                    (patient_id,)
                )
                
                plans = cursor.fetchall()
                if not plans:
                    print("No treatment plans found for patient")

                    return {
                        "completionRate": 0.0,
                        "weeklyStats": {},
                        "donutData": {"Completed": 0, "Partial": 0, "Missed": 0}
                    }
                
                cursor.execute(
                    """
                    SELECT 
                        COUNT(DISTINCT tpe.plan_exercise_id) as total_exercises,
                        COUNT(DISTINCT CASE 
                            WHEN EXISTS (
                                SELECT 1 FROM PatientExerciseProgress pep 
                                WHERE pep.plan_exercise_id = tpe.plan_exercise_id 
                                AND pep.patient_id = %s
                            ) THEN tpe.plan_exercise_id 
                            ELSE NULL 
                        END) as completed_exercises
                    FROM TreatmentPlanExercises tpe
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    WHERE tp.patient_id = %s
                    """,
                    (patient_id, patient_id)
                )
                
                overall_stats = cursor.fetchone()
                total_exercises = overall_stats["total_exercises"] or 0
                completed_exercises = overall_stats["completed_exercises"] or 0
                
                completion_rate = completed_exercises / total_exercises if total_exercises > 0 else 0
                print(f"Overall completion rate: {completed_exercises}/{total_exercises} = {completion_rate:.2f}")
                

                cursor.execute(
                    """
                    SELECT 
                        DAYNAME(completion_date) as day_of_week,
                        COUNT(DISTINCT plan_exercise_id) as exercises_completed
                    FROM PatientExerciseProgress
                    WHERE patient_id = %s AND completion_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)
                    GROUP BY DAYNAME(completion_date)
                    ORDER BY FIELD(DAYNAME(completion_date), 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')
                    """,
                    (patient_id,)
                )
                
                weekly_data = cursor.fetchall()
                weekly_stats = {}
                

                day_mapping = {
                    'Monday': 'Mon', 
                    'Tuesday': 'Tue', 
                    'Wednesday': 'Wed', 
                    'Thursday': 'Thu',
                    'Friday': 'Fri', 
                    'Saturday': 'Sat', 
                    'Sunday': 'Sun'
                }
                
                for day in weekly_data:
                    day_abbrev = day_mapping.get(day["day_of_week"], day["day_of_week"][:3])
                    weekly_stats[day_abbrev] = day["exercises_completed"]
                

                for abbrev in ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']:
                    if abbrev not in weekly_stats:
                        weekly_stats[abbrev] = 0
                

                cursor.execute(
                    """
                    SELECT
                        SUM(CASE WHEN EXISTS (
                            SELECT 1 FROM PatientExerciseProgress pep 
                            WHERE pep.plan_exercise_id = tpe.plan_exercise_id 
                            AND pep.patient_id = %s
                        ) THEN 1 ELSE 0 END) as completed,
                        SUM(CASE WHEN EXISTS (
                            SELECT 1 FROM PatientExerciseProgress pep 
                            WHERE pep.plan_exercise_id = tpe.plan_exercise_id 
                            AND pep.patient_id = %s
                            AND pep.sets_completed < tpe.sets
                        ) THEN 1 ELSE 0 END) as partial,
                        SUM(CASE WHEN NOT EXISTS (
                            SELECT 1 FROM PatientExerciseProgress pep 
                            WHERE pep.plan_exercise_id = tpe.plan_exercise_id 
                            AND pep.patient_id = %s
                        ) THEN 1 ELSE 0 END) as missed
                    FROM TreatmentPlanExercises tpe
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    WHERE tp.patient_id = %s AND tp.status = 'Active'
                    """,
                    (patient_id, patient_id, patient_id, patient_id)
                )
                
                donut_data = cursor.fetchone()
                

                result = {
                    "completionRate": completion_rate,
                    "weeklyStats": weekly_stats,
                    "donutData": {
                        "Completed": donut_data["completed"] or 0,
                        "Partial": donut_data["partial"] or 0,
                        "Missed": donut_data["missed"] or 0
                    }
                }
                
                print(f"Progress result: {result}")
                return result
                
            except Exception as e:
                print(f"Database error in get user progress: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get user progress: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
        
    @app.post("api/exercises/{planExerciseId}/update-status")
    async def update_exercise_status(
        request: Request, 
        plan_exercise_id: int,
        completed: bool
    ):
        """API endpoint to update the completion status of an exercise"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(status_code=404, content={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                
                cursor.execute(
                    """
                    SELECT tpe.* 
                    FROM TreatmentPlanExercises tpe
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    WHERE tpe.plan_exercise_id = %s AND tp.patient_id = %s
                    """,
                    (plan_exercise_id, patient_id)
                )
                exercise = cursor.fetchone()
                
                if not exercise:
                    return JSONResponse(
                        status_code=404, 
                        content={"detail": "Exercise not found or not associated with your treatment plans"}
                    )
                

                cursor.execute(
                    """
                    SELECT progress_id 
                    FROM PatientExerciseProgress 
                    WHERE plan_exercise_id = %s AND patient_id = %s
                    """,
                    (plan_exercise_id, patient_id)
                )
                progress = cursor.fetchone()
                

                if completed and not progress:
                    cursor.execute(
                        """
                        INSERT INTO PatientExerciseProgress 
                        (patient_id, plan_exercise_id, completion_date, sets_completed, repetitions_completed)
                        VALUES (%s, %s, CURRENT_DATE(), %s, %s)
                        """,
                        (
                            patient_id, 
                            plan_exercise_id, 
                            exercise["sets"],
                            exercise["repetitions"]
                        )
                    )
                    db.commit()
                    return {"detail": "Exercise marked as completed"}
                    

                elif not completed and progress:
                    cursor.execute(
                        """
                        DELETE FROM PatientExerciseProgress 
                        WHERE progress_id = %s
                        """,
                        (progress["progress_id"],)
                    )
                    db.commit()
                    return {"detail": "Exercise marked as pending"}
                    

                else:
                    return {"detail": "Exercise status already up to date"}
                
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error in update exercise status: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in update exercise status: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
    
    @app.get("/api/exercises/{exercise_id}")
    async def get_exercise_details(
        request: Request, 
        exercise_id: int
    ):
        """API endpoint to get detailed information about a specific exercise"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(status_code=404, conteCCnt={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                
                cursor.execute(
                    """
                    SELECT e.*, c.name as category_name
                    FROM Exercises e
                    LEFT JOIN ExerciseCategories c ON e.category_id = c.category_id
                    WHERE e.exercise_id = %s
                    """,
                    (exercise_id,)
                )
                exercise = cursor.fetchone()
                
                if not exercise:
                    return JSONResponse(status_code=404, content={"detail": "Exercise not found"})
                
                cursor.execute(
                    """
                    SELECT tpe.plan_exercise_id, tpe.plan_id, tpe.sets, tpe.repetitions, 
                        tpe.frequency, tpe.duration, tpe.notes,
                        tp.name as plan_name, tp.status as plan_status
                    FROM TreatmentPlanExercises tpe
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    WHERE tpe.exercise_id = %s AND tp.patient_id = %s
                    """,
                    (exercise_id, patient_id)
                )
                plan_exercises = cursor.fetchall()
                

                plan_exercise_instances = []
                for pe in plan_exercises:
                    cursor.execute(
                        """
                        SELECT * 
                        FROM PatientExerciseProgress 
                        WHERE plan_exercise_id = %s AND patient_id = %s
                        ORDER BY completion_date DESC
                        """,
                        (pe["plan_exercise_id"], patient_id)
                    )
                    progress = cursor.fetchall()
                    
                    plan_exercise_instances.append({
                        "planExerciseId": pe["plan_exercise_id"],
                        "planId": pe["plan_id"],
                        "planName": pe["plan_name"],
                        "planStatus": pe["plan_status"],
                        "sets": pe["sets"] or 3,
                        "repetitions": pe["repetitions"] or 10,
                        "frequency": pe["frequency"] or "Daily",
                        "duration": pe["duration"],
                        "notes": pe["notes"],
                        "completed": len(progress) > 0,
                        "progressHistory": [
                            {
                                "completionDate": p["completion_date"].isoformat() if p["completion_date"] else None,
                                "setsCompleted": p["sets_completed"],
                                "repetitionsCompleted": p["repetitions_completed"],
                                "durationSeconds": p["duration_seconds"],
                                "painLevel": p["pain_level"],
                                "difficultyLevel": p["difficulty_level"],
                                "notes": p["notes"]
                            } for p in progress
                        ]
                    })
                

                result = {
                    "exerciseId": exercise["exercise_id"],
                    "name": exercise["name"],
                    "description": exercise["description"] or "",
                    "videoUrl": exercise["video_url"],
                    "videoType": exercise["video_type"] or "",
                    "thumbnailUrl": exercise["video_filename"],
                    "difficulty": exercise["difficulty"] or "Beginner",
                    "categoryId": exercise["category_id"],
                    "categoryName": exercise["category_name"],
                    "duration": exercise["duration"],
                    "instructions": exercise["instructions"] or "",
                    "planInstances": plan_exercise_instances
                }
                
                return result
                
            except Exception as e:
                print(f"Database error in get exercise details: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get exercise details: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
    
            
    @app.post("/api/exercises/{plan_exercise_id}/progress")
    async def add_exercise_progress(
        request: Request, 
        plan_exercise_id: int,
        progress_request: ExerciseProgressRequest
    ):
        """API endpoint to add detailed progress for an exercise session"""
        print(f"Received progress for exercise {plan_exercise_id}: {progress_request}")
        
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            print(f"Authenticated user_id: {user_id}")
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    print(f"Patient not found for user_id: {user_id}")
                    return JSONResponse(status_code=404, content={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                print(f"Found patient_id: {patient_id}")
                

                cursor.execute(
                    """
                    SELECT tpe.*, tp.patient_id, e.name as exercise_name
                    FROM TreatmentPlanExercises tpe
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    JOIN Exercises e ON tpe.exercise_id = e.exercise_id
                    WHERE tpe.plan_exercise_id = %s
                    """,
                    (plan_exercise_id,)
                )
                exercise = cursor.fetchone()
                
                if not exercise:
                    print(f"Exercise not found: {plan_exercise_id}")
                    return JSONResponse(
                        status_code=404, 
                        content={"detail": "Exercise not found"}
                    )
                
                if exercise["patient_id"] != patient_id:
                    print(f"Permission denied: Exercise belongs to patient {exercise['patient_id']}, not {patient_id}")
                    return JSONResponse(
                        status_code=403, 
                        content={"detail": "You don't have permission to update this exercise"}
                    )
                
                print(f"Found exercise: {exercise['exercise_name']}, plan_id: {exercise['plan_id']}, exercise_id: {exercise['exercise_id']}")
                

                cursor.execute(
                    """
                    SELECT progress_id FROM PatientExerciseProgress
                    WHERE patient_id = %s AND plan_exercise_id = %s AND DATE(completion_date) = CURRENT_DATE()
                    """,
                    (patient_id, plan_exercise_id)
                )
                
                existing_progress = cursor.fetchone()
                print(f"Existing progress for today: {existing_progress}")
                
                if existing_progress:

                    cursor.execute(
                        """
                        UPDATE PatientExerciseProgress 
                        SET sets_completed = %s, 
                            repetitions_completed = %s, 
                            duration_seconds = %s, 
                            pain_level = %s, 
                            difficulty_level = %s, 
                            notes = %s
                        WHERE progress_id = %s
                        """,
                        (
                            progress_request.sets_completed,
                            progress_request.repetitions_completed,
                            progress_request.duration_seconds,
                            progress_request.pain_level,
                            progress_request.difficulty_level,
                            progress_request.notes,
                            existing_progress["progress_id"]
                        )
                    )
                    progress_id = existing_progress["progress_id"]
                    print(f"Updated existing progress entry: {progress_id}, rows affected: {cursor.rowcount}")
                else:

                    cursor.execute(
                        """
                        INSERT INTO PatientExerciseProgress 
                        (patient_id, plan_exercise_id, completion_date, sets_completed, 
                        repetitions_completed, duration_seconds, pain_level, difficulty_level, notes)
                        VALUES (%s, %s, CURRENT_DATE(), %s, %s, %s, %s, %s, %s)
                        """,
                        (
                            patient_id, 
                            plan_exercise_id, 
                            progress_request.sets_completed,
                            progress_request.repetitions_completed,
                            progress_request.duration_seconds,
                            progress_request.pain_level,
                            progress_request.difficulty_level,
                            progress_request.notes
                        )
                    )
                    progress_id = cursor.lastrowid
                    print(f"Inserted new progress entry: {progress_id}")
                
                db.commit()
                print(f"Database transaction committed")
                

                return {
                    "detail": "Exercise progress recorded successfully",
                    "progressId": progress_id,
                    "exerciseName": exercise["exercise_name"]
                }
                
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error in add exercise progress: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in add exercise progress: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
            
    @app.post("/api/exercises/{plan_exercise_id}/update-status")
    async def update_exercise_status(
        request: Request,
        plan_exercise_id: int,
        completed: bool
    ):
        """API endpoint to update the completion status of an exercise in a treatment plan"""
        print(f"Received update request for exercise {plan_exercise_id}, completed={completed}")
        
        session_id = request.cookies.get("session_id")
        if not session_id:
            print(f"No session_id found in cookies")
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                print(f"Invalid session data for {session_id}")
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            print(f"Authenticated user_id: {user_id}")
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    print(f"Patient not found for user_id: {user_id}")
                    return JSONResponse(status_code=404, content={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                print(f"Found patient_id: {patient_id}")
                

                cursor.execute(
                    """
                    SELECT tpe.*, tp.patient_id, tp.plan_id, e.name as exercise_name
                    FROM TreatmentPlanExercises tpe
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    JOIN Exercises e ON tpe.exercise_id = e.exercise_id
                    WHERE tpe.plan_exercise_id = %s
                    """,
                    (plan_exercise_id,)
                )
                exercise = cursor.fetchone()
                
                if not exercise:
                    print(f"Exercise not found: {plan_exercise_id}")
                    return JSONResponse(
                        status_code=404, 
                        content={"detail": "Exercise not found"}
                    )
                
                if exercise["patient_id"] != patient_id:
                    print(f"Permission denied: Exercise belongs to patient {exercise['patient_id']}, not {patient_id}")
                    return JSONResponse(
                        status_code=403, 
                        content={"detail": "You don't have permission to update this exercise"}
                    )
                
                plan_id = exercise["plan_id"]
                print(f"Found exercise: {exercise['exercise_name']}, plan_id: {plan_id}, exercise_id: {exercise['exercise_id']}")
                


                if completed:

                    cursor.execute(
                        """
                        SELECT progress_id FROM PatientExerciseProgress
                        WHERE patient_id = %s AND plan_exercise_id = %s AND DATE(completion_date) = CURRENT_DATE()
                        """,
                        (patient_id, plan_exercise_id)
                    )
                    
                    existing_progress = cursor.fetchone()
                    print(f"Existing progress for today: {existing_progress}")
                    
                    if not existing_progress:

                        sets = exercise.get("sets") or 3
                        repetitions = exercise.get("repetitions") or 10
                        

                        print(f"Inserting progress entry with sets={sets}, reps={repetitions}")
                        cursor.execute(
                            """
                            INSERT INTO PatientExerciseProgress 
                            (patient_id, plan_exercise_id, completion_date, sets_completed, repetitions_completed, 
                            pain_level, difficulty_level, duration_seconds, notes)
                            VALUES (%s, %s, CURRENT_DATE(), %s, %s, 0, 0, 0, 'Marked as completed via app')
                            """,
                            (patient_id, plan_exercise_id, sets, repetitions)
                        )
                        print(f"Inserted progress entry, row count: {cursor.rowcount}, last row ID: {cursor.lastrowid}")
                    else:
                        print(f"Progress entry already exists for today: {existing_progress['progress_id']}")
                else:

                    print(f"Deleting progress entries for today")
                    cursor.execute(
                        """
                        DELETE FROM PatientExerciseProgress
                        WHERE patient_id = %s AND plan_exercise_id = %s AND DATE(completion_date) = CURRENT_DATE()
                        """,
                        (patient_id, plan_exercise_id)
                    )
                    print(f"Deleted progress entries, row count: {cursor.rowcount}")
                
                db.commit()
                print(f"Database transaction committed")
                

                if completed:
                    cursor.execute(
                        """
                        SELECT COUNT(*) as count FROM PatientExerciseProgress
                        WHERE patient_id = %s AND plan_exercise_id = %s AND DATE(completion_date) = CURRENT_DATE()
                        """,
                        (patient_id, plan_exercise_id)
                    )
                    verify = cursor.fetchone()
                    print(f"Verification after commit: {verify['count']} progress entries found")
                


                cursor.execute(
                    """
                    SELECT 
                        COUNT(tpe.plan_exercise_id) as total_exercises,
                        SUM(CASE WHEN EXISTS (
                            SELECT 1 FROM PatientExerciseProgress pep 
                            WHERE pep.plan_exercise_id = tpe.plan_exercise_id 
                            AND pep.patient_id = %s
                        ) THEN 1 ELSE 0 END) as completed_exercises
                    FROM TreatmentPlanExercises tpe
                    WHERE tpe.plan_id = %s
                    """,
                    (patient_id, plan_id)
                )
                
                plan_progress = cursor.fetchone()
                total = plan_progress['total_exercises'] or 0
                completed_count = plan_progress['completed_exercises'] or 0
                

                completion_percentage = completed_count / total if total > 0 else 0
                print(f"Plan completion: {completed_count}/{total} = {completion_percentage:.2f}")
                

                if total > 0 and completed_count == total:
                    cursor.execute(
                        """
                        UPDATE TreatmentPlans 
                        SET status = 'Completed', updated_at = NOW() 
                        WHERE plan_id = %s AND status != 'Completed'
                        """,
                        (plan_id,)
                    )
                    plan_status_updated = cursor.rowcount > 0
                    if plan_status_updated:
                        print(f"Updated plan {plan_id} status to Completed")
                        db.commit()
                elif completed_count < total:

                    cursor.execute(
                        """
                        UPDATE TreatmentPlans 
                        SET status = 'Active', updated_at = NOW() 
                        WHERE plan_id = %s AND status = 'Completed'
                        """,
                        (plan_id,)
                    )
                    plan_status_updated = cursor.rowcount > 0
                    if plan_status_updated:
                        print(f"Updated plan {plan_id} status to Active")
                        db.commit()
                
                return {
                    "status": "success",
                    "message": f"Exercise marked as {'completed' if completed else 'pending'}",
                    "exercise_name": exercise['exercise_name'],
                    "plan_id": plan_id,
                    "plan_completion": {
                        "total": total,
                        "completed": completed_count,
                        "percentage": completion_percentage
                    }
                }
                
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error in update exercise status: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in update exercise status: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
    
    @app.post("/api/treatment-plans/{plan_id}/update-status")
    async def update_treatment_plan_status(
        request: Request,
        plan_id: int
    ):
        """API endpoint to check and update a treatment plan's status based on exercise completion"""
        print(f"Checking status for treatment plan {plan_id}")
        
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(status_code=404, content={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                

                cursor.execute(
                    """
                    SELECT * FROM TreatmentPlans 
                    WHERE plan_id = %s AND patient_id = %s
                    """,
                    (plan_id, patient_id)
                )
                plan = cursor.fetchone()
                
                if not plan:
                    return JSONResponse(
                        status_code=404, 
                        content={"detail": "Treatment plan not found or not associated with your account"}
                    )
                

                cursor.execute(
                    """
                    SELECT 
                        COUNT(tpe.plan_exercise_id) as total_exercises,
                        SUM(CASE WHEN EXISTS (
                            SELECT 1 FROM PatientExerciseProgress pep 
                            WHERE pep.plan_exercise_id = tpe.plan_exercise_id 
                            AND pep.patient_id = %s
                        ) THEN 1 ELSE 0 END) as completed_exercises
                    FROM TreatmentPlanExercises tpe
                    WHERE tpe.plan_id = %s
                    """,
                    (patient_id, plan_id)
                )
                
                result = cursor.fetchone()
                total = result["total_exercises"] or 0
                completed = result["completed_exercises"] or 0
                
                print(f"Plan has {completed}/{total} exercises completed")
                
                current_status = plan["status"]
                should_update = False
                new_status = current_status
                

                if total > 0 and completed == total:
                    if current_status != "Completed":
                        new_status = "Completed"
                        should_update = True
                        print("All exercises completed, updating plan status to Completed")
                else:
                    if current_status == "Completed":
                        new_status = "Active"
                        should_update = True
                        print("Not all exercises completed, updating plan status to Active")
                

                if should_update:
                    cursor.execute(
                        """
                        UPDATE TreatmentPlans 
                        SET status = %s, updated_at = NOW() 
                        WHERE plan_id = %s
                        """,
                        (new_status, plan_id)
                    )
                    db.commit()
                    print(f"Updated plan status to {new_status}")
                else:
                    print(f"No status update needed, keeping status as {current_status}")
                
                return {
                    "status": "success",
                    "plan_id": plan_id,
                    "total_exercises": total,
                    "completed_exercises": completed,
                    "current_status": new_status,
                    "was_updated": should_update
                }
                
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error in update treatment plan status: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in update treatment plan status: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
    
    @app.get("/api/treatment-plans/{plan_id}/progress")
    async def get_plan_progress(
        request: Request, 
        plan_id: int
    ):
        """API endpoint to get detailed progress summary for a specific treatment plan"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(status_code=404, content={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                

                cursor.execute(
                    "SELECT * FROM TreatmentPlans WHERE plan_id = %s AND patient_id = %s",
                    (plan_id, patient_id)
                )
                plan = cursor.fetchone()
                
                if not plan:
                    return JSONResponse(
                        status_code=404, 
                        content={"detail": "Treatment plan not found or not associated with your account"}
                    )
                

                cursor.execute(
                    """
                    SELECT 
                        COUNT(*) as total_exercises,
                        SUM(CASE WHEN pep.progress_id IS NOT NULL THEN 1 ELSE 0 END) as completed_exercises
                    FROM TreatmentPlanExercises tpe
                    LEFT JOIN PatientExerciseProgress pep ON 
                        tpe.plan_exercise_id = pep.plan_exercise_id AND
                        pep.patient_id = %s
                    WHERE tpe.plan_id = %s
                    """,
                    (patient_id, plan_id)
                )
                overall_stats = cursor.fetchone()
                

                cursor.execute(
                    """
                    SELECT 
                        pep.completion_date,
                        COUNT(DISTINCT pep.plan_exercise_id) as exercises_completed
                    FROM PatientExerciseProgress pep
                    JOIN TreatmentPlanExercises tpe ON pep.plan_exercise_id = tpe.plan_exercise_id
                    WHERE pep.patient_id = %s AND tpe.plan_id = %s
                    GROUP BY pep.completion_date
                    ORDER BY pep.completion_date DESC
                    LIMIT 30
                    """,
                    (patient_id, plan_id)
                )
                daily_activity = cursor.fetchall()
                

                cursor.execute(
                    """
                    SELECT 
                        e.exercise_id, e.name, 
                        tpe.plan_exercise_id,
                        tpe.sets, tpe.repetitions,
                        MAX(pep.completion_date) as last_completed,
                        COUNT(pep.progress_id) as completion_count,
                        AVG(pep.pain_level) as avg_pain,
                        AVG(pep.difficulty_level) as avg_difficulty
                    FROM TreatmentPlanExercises tpe
                    JOIN Exercises e ON tpe.exercise_id = e.exercise_id
                    LEFT JOIN PatientExerciseProgress pep ON 
                        tpe.plan_exercise_id = pep.plan_exercise_id AND
                        pep.patient_id = %s
                    WHERE tpe.plan_id = %s
                    GROUP BY e.exercise_id, tpe.plan_exercise_id
                    """,
                    (patient_id, plan_id)
                )
                exercise_progress = cursor.fetchall()
                

                start_date = plan["start_date"]
                today = datetime.datetime.now().date()
                days_active = (today - start_date).days if start_date else 0
                

                formatted_daily = []
                for day in daily_activity:
                    formatted_daily.append({
                        "date": day["completion_date"].isoformat(),
                        "exercisesCompleted": day["exercises_completed"]
                    })
                    
                formatted_exercises = []
                for ex in exercise_progress:
                    formatted_exercises.append({
                        "exerciseId": ex["exercise_id"],
                        "planExerciseId": ex["plan_exercise_id"],
                        "name": ex["name"],
                        "targetSets": ex["sets"] or 3,
                        "targetRepetitions": ex["repetitions"] or 10,
                        "lastCompleted": ex["last_completed"].isoformat() if ex["last_completed"] else None,
                        "completionCount": ex["completion_count"] or 0,
                        "averagePain": float(ex["avg_pain"]) if ex["avg_pain"] is not None else None,
                        "averageDifficulty": float(ex["avg_difficulty"]) if ex["avg_difficulty"] is not None else None,
                        "isCompleted": ex["completion_count"] > 0
                    })
                
                total_exercises = overall_stats["total_exercises"] or 0
                completed_exercises = overall_stats["completed_exercises"] or 0
                

                result = {
                    "planId": plan["plan_id"],
                    "planName": plan["name"],
                    "startDate": plan["start_date"].isoformat() if plan["start_date"] else None,
                    "endDate": plan["end_date"].isoformat() if plan["end_date"] else None,
                    "status": plan["status"],
                    "daysActive": days_active,
                    "totalExercises": total_exercises,
                    "completedExercises": completed_exercises,
                    "completionRate": completed_exercises / total_exercises if total_exercises > 0 else 0,
                    "dailyActivity": formatted_daily,
                    "exerciseProgress": formatted_exercises
                }
                
                return result
                
            except Exception as e:
                print(f"Database error in get plan progress: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get plan progress: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
    
    @app.get("/api/exercises/{exercise_id}/history")
    async def get_exercise_history(
        request: Request, 
        exercise_id: int
    ):
        """API endpoint to get history of all completions of a specific exercise across all treatment plans"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(status_code=404, content={"detail": "Patient profile not found"})
                
                patient_id = patient["patient_id"]
                

                cursor.execute(
                    "SELECT * FROM Exercises WHERE exercise_id = %s",
                    (exercise_id,)
                )
                exercise = cursor.fetchone()
                
                if not exercise:
                    return JSONResponse(status_code=404, content={"detail": "Exercise not found"})
                

                cursor.execute(
                    """
                    SELECT tpe.plan_exercise_id, tpe.plan_id, tp.name as plan_name, 
                        tpe.sets, tpe.repetitions, tpe.frequency
                    FROM TreatmentPlanExercises tpe
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    WHERE tpe.exercise_id = %s AND tp.patient_id = %s
                    """,
                    (exercise_id, patient_id)
                )
                plan_exercises = cursor.fetchall()
                
                if not plan_exercises:
                    return JSONResponse(
                        status_code=404, 
                        content={"detail": "Exercise not found in any of your treatment plans"}
                    )
                

                plan_exercise_ids = [pe["plan_exercise_id"] for pe in plan_exercises]
                placeholders = ', '.join(['%s'] * len(plan_exercise_ids))
                
                cursor.execute(
                    f"""
                    SELECT 
                        pep.*, tpe.plan_id, tp.name as plan_name
                    FROM PatientExerciseProgress pep
                    JOIN TreatmentPlanExercises tpe ON pep.plan_exercise_id = tpe.plan_exercise_id
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    WHERE pep.plan_exercise_id IN ({placeholders}) AND pep.patient_id = %s
                    ORDER BY pep.completion_date DESC, pep.created_at DESC
                    """,
                    tuple(plan_exercise_ids) + (patient_id,)
                )
                progress_entries = cursor.fetchall()
                

                cursor.execute(
                    f"""
                    SELECT 
                        COUNT(*) as total_completions,
                        AVG(pep.pain_level) as avg_pain,
                        AVG(pep.difficulty_level) as avg_difficulty,
                        MIN(pep.completion_date) as first_completed,
                        MAX(pep.completion_date) as last_completed
                    FROM PatientExerciseProgress pep
                    WHERE pep.plan_exercise_id IN ({placeholders}) AND pep.patient_id = %s
                    """,
                    tuple(plan_exercise_ids) + (patient_id,)
                )
                stats = cursor.fetchone()
                

                formatted_progress = []
                for entry in progress_entries:
                    formatted_progress.append({
                        "progressId": entry["progress_id"],
                        "planExerciseId": entry["plan_exercise_id"],
                        "planId": entry["plan_id"],
                        "planName": entry["plan_name"],
                        "completionDate": entry["completion_date"].isoformat() if entry["completion_date"] else None,
                        "setsCompleted": entry["sets_completed"],
                        "repetitionsCompleted": entry["repetitions_completed"],
                        "durationSeconds": entry["duration_seconds"],
                        "painLevel": entry["pain_level"],
                        "difficultyLevel": entry["difficulty_level"],
                        "notes": entry["notes"],
                        "createdAt": entry["created_at"].isoformat() if entry["created_at"] else None
                    })
                

                formatted_plans = []
                for pe in plan_exercises:
                    formatted_plans.append({
                        "planExerciseId": pe["plan_exercise_id"],
                        "planId": pe["plan_id"],
                        "planName": pe["plan_name"],
                        "sets": pe["sets"] or 3,
                        "repetitions": pe["repetitions"] or 10,
                        "frequency": pe["frequency"] or "Daily"
                    })
                

                result = {
                    "exerciseId": exercise["exercise_id"],
                    "name": exercise["name"],
                    "description": exercise["description"] or "",
                    "videoUrl": exercise["video_url"],
                    "videoType": exercise["video_type"] or "",
                    "difficulty": exercise["difficulty"] or "Beginner",
                    "stats": {
                        "totalCompletions": stats["total_completions"] or 0,
                        "averagePain": float(stats["avg_pain"]) if stats["avg_pain"] is not None else None,
                        "averageDifficulty": float(stats["avg_difficulty"]) if stats["avg_difficulty"] is not None else None,
                        "firstCompleted": stats["first_completed"].isoformat() if stats["first_completed"] else None,
                        "lastCompleted": stats["last_completed"].isoformat() if stats["last_completed"] else None
                    },
                    "planInstances": formatted_plans,
                    "progressHistory": formatted_progress
                }
                
                return result
                
            except Exception as e:
                print(f"Database error in get exercise history: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get exercise history: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
        
    @app.get("/api/therapists/{therapist_id}/details")
    async def get_therapist_details(therapist_id: int):
        """API endpoint to get detailed information about a specific therapist"""
        try:
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                cursor.execute(
                    """SELECT id, first_name, last_name, company_email, profile_image, 
                            bio, experience_years, specialties, education, languages, 
                            address, rating, review_count, 
                            is_accepting_new_patients, average_session_length
                    FROM Therapists 
                    WHERE id = %s""", 
                    (therapist_id,)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return JSONResponse(
                        status_code=404,
                        content={"detail": "Therapist not found"}
                    )
                
                for field in ['specialties', 'education', 'languages']:
                    therapist[field] = safely_parse_json_field(therapist[field], [])
                
                static_dir = getattr(app.state, 'static_directory', "/PERCEPTRONX/Frontend_Web/static")
                
                profile_image = therapist['profile_image']
                matched_image = find_best_matching_image(therapist_id, profile_image, static_dir)
                photo_url = f"/static/assets/images/user/{matched_image}"
                
                formatted_therapist = {
                    "id": therapist["id"],
                    "first_name": therapist["first_name"] or "",
                    "last_name": therapist["last_name"] or "",
                    "company_email": therapist["company_email"] or "",
                    "profile_image": therapist["profile_image"] or "",
                    "bio": therapist["bio"] or "",
                    "experience_years": therapist["experience_years"] or 0,
                    "specialties": therapist["specialties"],
                    "education": therapist["education"],
                    "languages": therapist["languages"],
                    "address": therapist["address"] or "",
                    "rating": float(therapist["rating"] or 0),
                    "review_count": therapist["review_count"] or 0,
                    "is_accepting_new_patients": bool(therapist["is_accepting_new_patients"]),
                    "average_session_length": therapist["average_session_length"] or 60,
                    "name": f"{therapist['first_name']} {therapist['last_name']}",
                    "photoUrl": photo_url
                }
                
                return formatted_therapist

            except Exception as e:
                print(f"Database error in get therapist details API: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Internal server error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get therapist details API: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )

    @app.get("/api/user/patient-profile")
    async def get_user_patient_profile(request: Request):
        """API endpoint to get the patient profile for the current logged-in user"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )

        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(
                    status_code=401,
                    content={"detail": "Not authenticated"}
                )

            user_id = session_data.user_id
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                cursor.execute(
                    "SELECT * FROM Patients WHERE user_id = %s",
                    (user_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(
                        status_code=404,
                        content={"detail": "Patient profile not found"}
                    )
                
                formatted_patient = {
                    "patient_id": patient["patient_id"],
                    "therapist_id": patient["therapist_id"],
                    "first_name": patient["first_name"] or "",
                    "last_name": patient["last_name"] or "",
                    "email": patient["email"] or "",
                    "phone": patient["phone"] or "",
                    "date_of_birth": patient["date_of_birth"].isoformat() if patient["date_of_birth"] else "",
                    "address": patient["address"] or "",
                    "diagnosis": patient["diagnosis"] or "",
                    "status": patient["status"] or "Active",
                    "notes": patient["notes"] or "",
                    "created_at": patient["created_at"].isoformat() if patient["created_at"] else "",
                    "updated_at": patient["updated_at"].isoformat() if patient["updated_at"] else "",
                    "user_id": str(patient["user_id"]) if patient["user_id"] else ""
                }
                
                return formatted_patient

            except Exception as e:
                print(f"Database error in get user patient profile API: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Internal server error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get user patient profile API: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
            
    @app.post("/treatment-plans/delete")
    async def delete_treatment_plan(request: Request, user=Depends(get_current_user)):
        form_data = await request.form()
        plan_id = form_data.get("plan_id")
        
        if not plan_id:
            return RedirectResponse(url="/treatment-plans", status_code=303)
        
        db = get_Mysql_db()
        cursor = db.cursor()
        try:
            cursor.execute(
                "DELETE FROM TreatmentPlans WHERE plan_id = %s AND therapist_id = %s",
                (plan_id, user["user_id"])
            )
            db.commit()
            
            return RedirectResponse(url="/treatment-plans", status_code=303)
        except Exception as e:
            print(f"Error deleting treatment plan: {e}")
            return RedirectResponse(url="/treatment-plans", status_code=303)
        finally:
            cursor.close()
            db.close()

    @app.get("/api/appointments/{appointment_id}")
    async def get_appointment_details(appointment_id: int ):
        """API endpoint to get detailed information about a specific appointment"""
        try:
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    "SELECT * FROM Appointments WHERE appointment_id = %s",
                    (appointment_id,)
                )
                appointment = cursor.fetchone()
                
                if not appointment:
                    return JSONResponse(
                        status_code=404,
                        content={"detail": "Appointment not found"}
                    )
                    
                notes_info = {"Type": "", "Notes": "", "Insurance": "", "Member_ID": 0}
                if appointment["notes"]:
                    lines = appointment["notes"].split('\n')
                    for line in lines:
                        if line.startswith('Type:'):
                            notes_info["Type"] = line[5:].strip()
                        elif line.startswith('Notes:'):
                            notes_info["Notes"] = line[6:].strip()
                        elif line.startswith('Insurance:'):
                            notes_info["Insurance"] = line[10:].strip()
                        elif line.startswith('Member ID:'):
                            id_str = line[10:].strip()
                            try:
                                notes_info["Member_ID"] = int(id_str)
                            except ValueError:
                                notes_info["Member_ID"] = 0
                
                formatted_appointment = {
                    "appointment_id": appointment["appointment_id"],
                    "patient_id": appointment["patient_id"],
                    "therapist_id": appointment["therapist_id"],
                    "appointment_date": appointment["appointment_date"].isoformat() if appointment["appointment_date"] else "",
                    "appointment_time": format_mysql_time(appointment["appointment_time"]),
                    "duration": appointment["duration"] or 60,
                    "status": appointment["status"] or "Scheduled",
                    "notes": appointment["notes"] or "",
                    "Type": notes_info["Type"],
                    "Notes": notes_info["Notes"],
                    "Insurance": notes_info["Insurance"],
                    "Member_ID": notes_info["Member_ID"],
                    "created_at": appointment["created_at"].isoformat() if appointment["created_at"] else "",
                    "updated_at": appointment["updated_at"].isoformat() if appointment["updated_at"] else ""
                }
                
                return formatted_appointment

            except Exception as e:
                print(f"Database error in get appointment details API: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Internal server error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get appointment details API: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )

    @app.get("/api/patients/{patient_id}/appointments")
    async def get_patient_appointments(patient_id: int):
        """API endpoint to get all appointments for a specific patient"""
        try:
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    "SELECT patient_id FROM Patients WHERE patient_id = %s",
                    (patient_id,)
                )
                patient = cursor.fetchone()
                
                if not patient:
                    return JSONResponse(
                        status_code=404,
                        content={"detail": "Patient not found"}
                    )
                
                cursor.execute(
                    """SELECT * FROM Appointments 
                    WHERE patient_id = %s
                    ORDER BY appointment_date DESC, appointment_time DESC""",
                    (patient_id,)
                )
                appointments = cursor.fetchall()
                
                formatted_appointments = []
                for appointment in appointments:
                    notes_info = {"Type": "", "Notes": "", "Insurance": "", "Member_ID": 0}
                    if appointment["notes"]:
                        lines = appointment["notes"].split('\n')
                        for line in lines:
                            if line.startswith('Type:'):
                                notes_info["Type"] = line[5:].strip()
                            elif line.startswith('Notes:'):
                                notes_info["Notes"] = line[6:].strip()
                            elif line.startswith('Insurance:'):
                                notes_info["Insurance"] = line[10:].strip()
                            elif line.startswith('Member ID:'):
                                id_str = line[10:].strip()
                                try:
                                    notes_info["Member_ID"] = int(id_str)
                                except ValueError:
                                    notes_info["Member_ID"] = 0
                    
                    formatted_appointment = {
                        "appointment_id": appointment["appointment_id"],
                        "patient_id": appointment["patient_id"],
                        "therapist_id": appointment["therapist_id"],
                        "appointment_date": appointment["appointment_date"].isoformat() if appointment["appointment_date"] else "",
                        "appointment_time": format_mysql_time(appointment["appointment_time"]),
                        "duration": appointment["duration"] or 60,
                        "status": appointment["status"] or "Scheduled",
                        "notes": appointment["notes"] or "",
                        "Type": notes_info["Type"],
                        "Notes": notes_info["Notes"],
                        "Insurance": notes_info["Insurance"],
                        "Member_ID": notes_info["Member_ID"],
                        "created_at": appointment["created_at"].isoformat() if appointment["created_at"] else "",
                        "updated_at": appointment["updated_at"].isoformat() if appointment["updated_at"] else ""
                    }
                    
                    formatted_appointments.append(formatted_appointment)
                
                return formatted_appointments

            except Exception as e:
                print(f"Database error in get patient appointments API: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Internal server error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get patient appointments API: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )

    @app.get("/exercises/{exercise_id}")
    async def view_exercise(request: Request, exercise_id: int, user = Depends(get_current_user)):
        """Display detailed view of a specific exercise"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT id, first_name, last_name FROM Therapists WHERE id = %s", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")
                

                cursor.execute(
                    """SELECT e.*, c.name as category_name 
                    FROM Exercises e
                    LEFT JOIN ExerciseCategories c ON e.category_id = c.category_id
                    WHERE e.exercise_id = %s""", 
                    (exercise_id,)
                )
                exercise = cursor.fetchone()
                
                if not exercise:
                    return RedirectResponse(url="/exercises?error=not_found")
                

                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0
                

                cursor.execute(
                    """SELECT tpe.*, tp.name, tp.plan_id,
                            CONCAT(p.first_name, ' ', p.last_name) as patient_name
                    FROM TreatmentPlanExercises tpe
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    JOIN Patients p ON tp.patient_id = p.patient_id
                    WHERE tpe.exercise_id = %s
                    ORDER BY tp.created_at DESC""",
                    (exercise_id,)
                )
                plans_using_exercise = cursor.fetchall()
                

                cursor.execute(
                    """SELECT tp.plan_id, tp.name, 
                            CONCAT(p.first_name, ' ', p.last_name) as patient_name
                    FROM TreatmentPlans tp
                    JOIN Patients p ON tp.patient_id = p.patient_id
                    WHERE tp.therapist_id = %s AND tp.status = 'Active'
                    ORDER BY tp.name""",
                    (session_data["user_id"],)
                )
                treatment_plans = cursor.fetchall()
                

                if exercise['instructions'] and isinstance(exercise['instructions'], str):
                    exercise['instructions'] = exercise['instructions'].strip()
                


                exercise['recommendations'] = []
                

                if exercise['video_url'] and 'youtube.com' in exercise['video_url']:

                    if 'watch?v=' in exercise['video_url']:
                        video_id = exercise['video_url'].split('watch?v=')[1].split('&')[0]
                        exercise['video_url'] = f"https://www.youtube.com/embed/{video_id}"
                
                category_name = exercise['category_name'] if 'category_name' in exercise else None
                
                therapist_data = await get_therapist_data(user["user_id"])

                
                return templates.TemplateResponse(
                    "dist/exercises/view_exercise.html",
                    {
                        "request": request,
                        "exercise": exercise,
                        "therapist": therapist_data,
                        "category_name": category_name,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "plans_using_exercise": plans_using_exercise,
                        "treatment_plans": treatment_plans
                    }
                )
                    
            except Exception as e:
                print(f"Database error in view exercise: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(url="/exercises?error=database")
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in view exercise: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")

    @app.post("/treatment-plans/add-exercise")
    async def add_exercise_to_plan(
        request: Request,
        exercise_id: int = Form(...),
        plan_id: int = Form(...),
        sets: int = Form(3),
        repetitions: int = Form(10),
        frequency: str = Form("Every other day"),
        notes: str = Form(None)
    ):
        """Route to add an exercise to an existing treatment plan"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor()
                

                cursor.execute(
                    "SELECT plan_id FROM TreatmentPlans WHERE plan_id = %s AND therapist_id = %s",
                    (plan_id, session_data["user_id"])
                )
                if not cursor.fetchone():
                    return RedirectResponse(url=f"/exercises/{exercise_id}?error=unauthorized_plan")
                    

                cursor.execute(
                    "SELECT exercise_id FROM Exercises WHERE exercise_id = %s",
                    (exercise_id,)
                )
                if not cursor.fetchone():
                    return RedirectResponse(url=f"/exercises/{exercise_id}?error=invalid_exercise")
                    

                cursor.execute(
                    "SELECT plan_exercise_id FROM TreatmentPlanExercises WHERE plan_id = %s AND exercise_id = %s",
                    (plan_id, exercise_id)
                )
                existing = cursor.fetchone()
                
                if existing:

                    cursor.execute(
                        """UPDATE TreatmentPlanExercises 
                        SET sets = %s, repetitions = %s, frequency = %s, notes = %s
                        WHERE plan_id = %s AND exercise_id = %s""",
                        (sets, repetitions, frequency, notes, plan_id, exercise_id)
                    )
                    message = "updated"
                else:

                    cursor.execute(
                        """INSERT INTO TreatmentPlanExercises 
                        (plan_id, exercise_id, sets, repetitions, frequency, notes) 
                        VALUES (%s, %s, %s, %s, %s, %s)""",
                        (plan_id, exercise_id, sets, repetitions, frequency, notes)
                    )
                    message = "added"
                    
                db.commit()
                
                return RedirectResponse(
                    url=f"/exercises/{exercise_id}?success=exercise_{message}_to_plan", 
                    status_code=303
                )
                    
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error adding exercise to plan: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return RedirectResponse(
                    url=f"/exercises/{exercise_id}?error=database", 
                    status_code=303
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error adding exercise to plan: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return RedirectResponse(url="/Therapist_Login")

    @app.get("/api/treatment-plans/active")
    async def get_active_treatment_plans(request: Request):
        """API endpoint to get active treatment plans for the current therapist"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(status_code=401, content={"error": "Not authenticated"})

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"error": "Not authenticated"})

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                
                cursor.execute(
                    """SELECT tp.plan_id, tp.name, 
                            CONCAT(p.first_name, ' ', p.last_name) as patient_name
                    FROM TreatmentPlans tp
                    JOIN Patients p ON tp.patient_id = p.patient_id
                    WHERE tp.therapist_id = %s AND tp.status = 'Active'
                    ORDER BY tp.name""",
                    (session_data["user_id"],)
                )
                treatment_plans = cursor.fetchall()
                
                return treatment_plans
                    
            except Exception as e:
                print(f"Database error in get active treatment plans API: {e}")
                return JSONResponse(status_code=500, content={"error": "Database error"})
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        except Exception as e:
            print(f"Error in get active treatment plans API: {e}")
            return JSONResponse(status_code=500, content={"error": "Server error"})
            
    @app.get("/patients")
    async def get_patients_page(request: Request, user=Depends(get_current_user)):
        db = get_Mysql_db()
        cursor = db.cursor(dictionary=True)

        try:
            cursor.execute(
                "SELECT * FROM Patients WHERE therapist_id = %s ORDER BY last_name", 
                (user["user_id"],)
            )
            patients = cursor.fetchall()

            therapist_data = await get_therapist_data(user["user_id"])

            return templates.TemplateResponse(
                "dist/dashboard/patient_directory.html", 
                {
                    "request": request,
                    "patients": patients,
                    "therapist": therapist_data,
                    "first_name": therapist_data["first_name"],
                    "last_name": therapist_data["last_name"]
                }
            )
        finally:
            cursor.close()
            db.close()

    @app.get("/patients/add")
    async def add_patient_page(request: Request, user=Depends(get_current_user)):
        therapist_data = await get_therapist_data(user["user_id"])

        return templates.TemplateResponse(
            "dist/dashboard/add_patient.html", 
            {
                "request": request,
                "therapist": therapist_data,
                "first_name": therapist_data["first_name"],
                "last_name": therapist_data["last_name"]
            }
        )
        
    @app.get("/patients/{patient_id}/metrics")
    async def patient_metrics(request: Request, patient_id: int):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:
                now = datetime.datetime.now()
                today = datetime.date.today()

                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image
                    FROM Therapists 
                    WHERE id = %s""", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()

                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")

                cursor.execute(
                    """SELECT * FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                patient = cursor.fetchone()

                if not patient:
                    return RedirectResponse(url="/patients")

                cursor.execute(
                    """SELECT * FROM PatientMetrics
                    WHERE patient_id = %s
                    ORDER BY measurement_date DESC""",
                    (patient_id,)
                )
                patient_metrics = cursor.fetchall()

                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0

                cursor.execute(
                    """SELECT 
                        ROUND(AVG(pain_level), 1) as avg_pain_level,
                        ROUND(AVG(functionality_score), 1) as avg_functionality_score,
                        ROUND(AVG(adherence_rate), 1) as avg_adherence_rate,
                        ROUND(AVG(recovery_progress), 1) as avg_recovery_progress
                    FROM PatientMetrics 
                    WHERE patient_id = %s
                    AND measurement_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)""",
                    (patient_id,)
                )
                monthly_averages = cursor.fetchone()

                cursor.execute(
                    """SELECT 
                        pain_level, 
                        functionality_score, 
                        adherence_rate, 
                        recovery_progress
                    FROM PatientMetrics
                    WHERE patient_id = %s
                    ORDER BY measurement_date DESC
                    LIMIT 1, 1""",
                    (patient_id,)
                )
                previous_metrics = cursor.fetchone()

                cursor.execute(
                    """SELECT 
                        pain_level, 
                        functionality_score, 
                        adherence_rate, 
                        recovery_progress, 
                        measurement_date
                    FROM PatientMetrics
                    WHERE patient_id = %s
                    ORDER BY measurement_date DESC
                    LIMIT 1""",
                    (patient_id,)
                )
                latest_metrics = cursor.fetchone()

                chart_dates = []
                pain_data = []
                functionality_data = []
                adherence_data = []
                recovery_data = []

                for metric in patient_metrics:
                    chart_dates.insert(0, metric['measurement_date'].strftime('%Y-%m-%d'))
                    pain_data.insert(0, metric['pain_level'])
                    functionality_data.insert(0, metric['functionality_score'])
                    adherence_data.insert(0, metric['adherence_rate'])
                    recovery_data.insert(0, metric['recovery_progress'])

                trends = {}
                if previous_metrics and latest_metrics:
                    trends = {
                        'pain': {
                            'change': latest_metrics['pain_level'] - previous_metrics['pain_level'] if latest_metrics['pain_level'] is not None and previous_metrics['pain_level'] is not None else 0,
                            'direction': 'down' if latest_metrics['pain_level'] < previous_metrics['pain_level'] else 'up' if latest_metrics['pain_level'] is not None and previous_metrics['pain_level'] is not None else 'same',
                            'color': 'success' if latest_metrics['pain_level'] < previous_metrics['pain_level'] else 'danger' if latest_metrics['pain_level'] is not None and previous_metrics['pain_level'] is not None else 'secondary'
                        },
                        'functionality': {
                            'change': latest_metrics['functionality_score'] - previous_metrics['functionality_score'] if latest_metrics['functionality_score'] is not None and previous_metrics['functionality_score'] is not None else 0,
                            'direction': 'up' if latest_metrics['functionality_score'] > previous_metrics['functionality_score'] else 'down' if latest_metrics['functionality_score'] is not None and previous_metrics['functionality_score'] is not None else 'same',
                            'color': 'success' if latest_metrics['functionality_score'] > previous_metrics['functionality_score'] else 'danger' if latest_metrics['functionality_score'] is not None and previous_metrics['functionality_score'] is not None else 'secondary'
                        },
                        'adherence': {
                            'change': latest_metrics['adherence_rate'] - previous_metrics['adherence_rate'] if latest_metrics['adherence_rate'] is not None and previous_metrics['adherence_rate'] is not None else 0,
                            'direction': 'up' if latest_metrics['adherence_rate'] > previous_metrics['adherence_rate'] else 'down' if latest_metrics['adherence_rate'] is not None and previous_metrics['adherence_rate'] is not None else 'same',
                            'color': 'success' if latest_metrics['adherence_rate'] > previous_metrics['adherence_rate'] else 'danger' if latest_metrics['adherence_rate'] is not None and previous_metrics['adherence_rate'] is not None else 'secondary'
                        },
                        'recovery': {
                            'change': latest_metrics['recovery_progress'] - previous_metrics['recovery_progress'] if latest_metrics['recovery_progress'] is not None and previous_metrics['recovery_progress'] is not None else 0,
                            'direction': 'up' if latest_metrics['recovery_progress'] > previous_metrics['recovery_progress'] else 'down' if latest_metrics['recovery_progress'] is not None and previous_metrics['recovery_progress'] is not None else 'same',
                            'color': 'success' if latest_metrics['recovery_progress'] > previous_metrics['recovery_progress'] else 'danger' if latest_metrics['recovery_progress'] is not None and previous_metrics['recovery_progress'] is not None else 'secondary'
                        }
                    }

                return templates.TemplateResponse(
                    "dist/dashboard/patient_metrics.html",
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "patient": patient,
                        "patient_metrics": patient_metrics,
                        "monthly_averages": monthly_averages,
                        "latest_metrics": latest_metrics,
                        "trends": trends,
                        "chart_data": {
                            "dates": chart_dates,
                            "pain": pain_data,
                            "functionality": functionality_data,
                            "adherence": adherence_data,
                            "recovery": recovery_data
                        },
                        "today": today,
                        "now": now  
                    }
                )

            except Exception as e:
                print(f"Database error in patient metrics: {e}")
                print(traceback.format_exc())  
                return RedirectResponse(url=f"/patients/{patient_id}")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in patient metrics: {e}")
            print(traceback.format_exc()) 
            return RedirectResponse(url="/Therapist_Login")
    
    @app.post("/patients/{patient_id}/general-note")
    async def update_patient_general_note(request: Request, patient_id: int, notes: str = Form(...)):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor()

            try:

                cursor.execute(
                    """SELECT patient_id FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    return RedirectResponse(url="/patients")

                cursor.execute(
                    """UPDATE Patients 
                    SET notes = %s 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (notes, patient_id, session_data["user_id"])
                )
                
                db.commit()
                
                return RedirectResponse(url=f"/patients/{patient_id}", status_code=303)

            except Exception as e:
                print(f"Database error updating patient general note: {e}")
                db.rollback()
                return RedirectResponse(url=f"/patients/{patient_id}")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error updating patient general note: {e}")
            return RedirectResponse(url="/Therapist_Login")
        
    @app.post("/patients/{patient_id}/metrics")
    async def add_patient_metrics_legacy(
        request: Request, 
        patient_id: int,
        metric_type: str = Form(...),
        value: str = Form(...),
        unit: Optional[str] = Form(None),
        measurement_date: str = Form(...),
        notes: Optional[str] = Form(None)
    ):
        """
        Legacy route handler to support the existing form in patient details.
        This converts the old metric format to the new PatientMetrics table format.
        """
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:

                cursor.execute(
                    """SELECT patient_id FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    return RedirectResponse(url="/patients")

                pain_level = None
                functionality_score = None
                adherence_rate = None
                recovery_progress = None
                
                try:
                    metric_value = float(value)
                    
                    if metric_type == "pain_level":
                        pain_level = min(10, max(0, int(metric_value)))
                    elif metric_type == "range_of_motion":
                        functionality_score = min(100, max(0, int(metric_value)))
                    elif metric_type == "heart_rate" or metric_type == "blood_pressure" or metric_type == "weight":


                        notes = f"{metric_type}: {value} {unit or ''}" + (f"\n{notes}" if notes else "")

                    else:
                        notes = f"{metric_type}: {value} {unit or ''}" + (f"\n{notes}" if notes else "")
                except ValueError:

                    notes = f"{metric_type}: {value} {unit or ''}" + (f"\n{notes}" if notes else "")


                cursor.execute(
                    """INSERT INTO PatientMetrics 
                    (patient_id, therapist_id, measurement_date, pain_level, 
                    functionality_score, adherence_rate, recovery_progress, notes)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)""",
                    (
                        patient_id, 
                        session_data["user_id"], 
                        measurement_date,
                        pain_level,
                        functionality_score,
                        adherence_rate,
                        recovery_progress,
                        notes
                    )
                )
                
                db.commit()
                
                return RedirectResponse(url=f"/patients/{patient_id}", status_code=303)

            except Exception as e:
                print(f"Database error in adding patient metrics: {e}")
                return RedirectResponse(url=f"/patients/{patient_id}")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in adding patient metrics: {e}")
            return RedirectResponse(url="/Therapist_Login")
            
    @app.get("/patients/{patient_id}/notes")
    async def patient_notes(request: Request, patient_id: int):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:

                cursor.execute(
                    """SELECT id, first_name, last_name, profile_image
                    FROM Therapists 
                    WHERE id = %s""", 
                    (session_data["user_id"],)
                )
                therapist = cursor.fetchone()

                if not therapist:
                    return RedirectResponse(url="/Therapist_Login")


                cursor.execute(
                    """SELECT * FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                patient = cursor.fetchone()

                if not patient:
                    return RedirectResponse(url="/patients")

                cursor.execute(
                    """SELECT 
                        pn.note_id, 
                        pn.patient_id, 
                        pn.therapist_id, 
                        pn.appointment_id, 
                        pn.note_text,
                        pn.created_at,
                        pn.updated_at,
                        t.first_name,
                        t.last_name
                    FROM PatientNotes pn
                    LEFT JOIN Therapists t ON pn.therapist_id = t.id
                    WHERE pn.patient_id = %s
                    ORDER BY pn.created_at DESC""",
                    (patient_id,)
                )
                
                patient_notes_raw = cursor.fetchall()
                patient_notes = []
                

                for note in patient_notes_raw:

                    processed_note = {}
                    

                    for key, value in note.items():

                        if isinstance(value, datetime.datetime):
                            processed_note[key] = value.strftime('%Y-%m-%d %H:%M:%S')
                        elif isinstance(value, datetime.date):
                            processed_note[key] = value.strftime('%Y-%m-%d')
                        elif isinstance(value, datetime.timedelta):
                            total_seconds = int(value.total_seconds())
                            hours, remainder = divmod(total_seconds, 3600)
                            minutes, seconds = divmod(remainder, 60)
                            processed_note[key] = f"{hours:02d}:{minutes:02d}"
                        else:
                            processed_note[key] = value
                    

                    patient_notes.append(processed_note)
                
                appointment_ids = [note['appointment_id'] for note in patient_notes 
                                if note.get('appointment_id') is not None]
                
                if appointment_ids:

                    placeholders = ', '.join(['%s'] * len(appointment_ids))
                    cursor.execute(
                        f"""SELECT 
                            appointment_id, 
                            appointment_date, 
                            appointment_time 
                        FROM Appointments 
                        WHERE appointment_id IN ({placeholders})""",
                        tuple(appointment_ids)
                    )
                    
                    appointments_data = cursor.fetchall()
                    appointments_dict = {}

                    for appt in appointments_data:
                        appt_date = appt['appointment_date']
                        appt_time = appt['appointment_time']
                        

                        if isinstance(appt_date, (datetime.date, datetime.datetime)):
                            date_str = appt_date.strftime('%Y-%m-%d')
                        else:
                            date_str = str(appt_date)
                            
                        if isinstance(appt_time, datetime.timedelta):
                            total_seconds = int(appt_time.total_seconds())
                            hours, remainder = divmod(total_seconds, 3600)
                            minutes, seconds = divmod(remainder, 60)
                            time_str = f"{hours:02d}:{minutes:02d}"
                        else:
                            time_str = str(appt_time)
                        
                        appointments_dict[appt['appointment_id']] = {
                            'appointment_date': date_str,
                            'appointment_time': time_str
                        }
                    

                    for note in patient_notes:
                        if note.get('appointment_id') in appointments_dict:
                            note['appointment_date'] = appointments_dict[note['appointment_id']]['appointment_date']
                            note['appointment_time'] = appointments_dict[note['appointment_id']]['appointment_time']


                cursor.execute(
                    """SELECT * FROM Appointments
                    WHERE patient_id = %s AND therapist_id = %s
                    ORDER BY appointment_date DESC, appointment_time DESC""",
                    (patient_id, session_data["user_id"])
                )
                
                appointments_raw = cursor.fetchall()
                appointments = []
                
                for appt in appointments_raw:
                    processed_appt = {}
                    
                    for key, value in appt.items():
                        if isinstance(value, datetime.datetime):
                            processed_appt[key] = value.strftime('%Y-%m-%d %H:%M:%S')
                        elif isinstance(value, datetime.date):
                            processed_appt[key] = value.strftime('%Y-%m-%d')
                        elif isinstance(value, datetime.timedelta):
                            total_seconds = int(value.total_seconds())
                            hours, remainder = divmod(total_seconds, 3600)
                            minutes, seconds = divmod(remainder, 60)
                            processed_appt[key] = f"{hours:02d}:{minutes:02d}"
                        else:
                            processed_appt[key] = value
                    
                    appointments.append(processed_appt)

                cursor.execute(
                    "SELECT COUNT(*) as count FROM Messages WHERE recipient_id = %s AND recipient_type = 'therapist' AND is_read = FALSE",
                    (session_data["user_id"],)
                )
                unread_count_result = cursor.fetchone()
                unread_messages_count = unread_count_result['count'] if unread_count_result else 0


                print("Formatted patient notes:")
                if patient_notes and len(patient_notes) > 0:
                    sample_note = patient_notes[0]
                    for key, value in sample_note.items():
                        print(f"{key}: {type(value).__name__} = {value}")

                return templates.TemplateResponse(
                    "dist/dashboard/patient_notes.html",
                    {
                        "request": request,
                        "therapist": therapist,
                        "first_name": therapist["first_name"],
                        "last_name": therapist["last_name"],
                        "unread_messages_count": unread_messages_count,
                        "patient": patient,
                        "patient_notes": patient_notes,
                        "appointments": appointments
                    }
                )

            except Exception as e:
                print(f"Database error in patient notes: {e}")
                print(traceback.format_exc()) 
                return RedirectResponse(url=f"/patients/{patient_id}")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in patient notes: {e}")
            print(traceback.format_exc()) 
            return RedirectResponse(url="/Therapist_Login")

    @app.post("/patients/{patient_id}/notes")
    async def add_patient_note(request: Request, patient_id: int, 
                            note_text: str = Form(...),
                            appointment_id: Optional[int] = Form(None)):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor()

            try:

                cursor.execute(
                    """SELECT patient_id FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    return RedirectResponse(url="/patients")


                cursor.execute(
                    """INSERT INTO PatientNotes (patient_id, therapist_id, appointment_id, note_text)
                    VALUES (%s, %s, %s, %s)""",
                    (patient_id, session_data["user_id"], appointment_id, note_text)
                )
                
                db.commit()
                

                return RedirectResponse(url=f"/patients/{patient_id}", status_code=303)

            except Exception as e:
                print(f"Database error in adding patient note: {e}")

                return RedirectResponse(url=f"/patients/{patient_id}")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in adding patient note: {e}")
            return RedirectResponse(url="/Therapist_Login")
        
    @app.post("/patients/{patient_id}/metrics/add")
    async def add_patient_metrics(
        request: Request, 
        patient_id: int,
        measurement_date: str = Form(...),
        pain_level: int = Form(...),
        functionality_score: int = Form(...),
        adherence_rate: float = Form(...),
        recovery_progress: float = Form(...),
        notes: Optional[str] = Form(None)
    ):
        session_id = request.cookies.get("session_id")
        if not session_id:
            return RedirectResponse(url="/Therapist_Login")

        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return RedirectResponse(url="/Therapist_Login")

            db = get_Mysql_db()
            cursor = db.cursor(dictionary=True)

            try:

                cursor.execute(
                    """SELECT patient_id FROM Patients 
                    WHERE patient_id = %s AND therapist_id = %s""",
                    (patient_id, session_data["user_id"])
                )
                
                if not cursor.fetchone():
                    return RedirectResponse(url="/patients")

                cursor.execute(
                    """INSERT INTO PatientMetrics 
                    (patient_id, therapist_id, measurement_date, pain_level, 
                    functionality_score, adherence_rate, recovery_progress, notes)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)""",
                    (
                        patient_id, 
                        session_data["user_id"], 
                        measurement_date,
                        pain_level,
                        functionality_score,
                        adherence_rate,
                        recovery_progress,
                        notes
                    )
                )
                
                db.commit()
                
                return RedirectResponse(url=f"/patients/{patient_id}/metrics", status_code=303)

            except Exception as e:
                print(f"Database error in adding patient metrics: {e}")
                return RedirectResponse(url=f"/patients/{patient_id}/metrics")
            finally:
                cursor.close()
                db.close()
        except Exception as e:
            print(f"Error in adding patient metrics: {e}")
            return RedirectResponse(url="/Therapist_Login")
    
    
    @app.get("/api/debug/treatment-plans/{patient_id}")
    async def debug_treatment_plans(
        request: Request,
        patient_id: int
    ):
        """API endpoint to list all treatment plans for a patient (admin/debug only)"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        
        try:
            session_data = await get_session_data(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})

            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    """
                    SELECT plan_id, name, status, patient_id, therapist_id, created_at
                    FROM TreatmentPlans
                    WHERE patient_id = %s
                    """,
                    (patient_id,)
                )
                
                plans = cursor.fetchall()
                

                cursor.execute(
                    """
                    SELECT tpe.plan_exercise_id, tpe.plan_id, tpe.exercise_id, e.name as exercise_name,
                        tp.name as plan_name, tp.patient_id
                    FROM TreatmentPlanExercises tpe
                    JOIN Exercises e ON tpe.exercise_id = e.exercise_id
                    JOIN TreatmentPlans tp ON tpe.plan_id = tp.plan_id
                    WHERE tp.patient_id = %s
                    """,
                    (patient_id,)
                )
                
                plan_exercises = cursor.fetchall()

                for plan in plans:
                    if plan["created_at"]:
                        plan["created_at"] = plan["created_at"].isoformat()
                
                return {
                    "patient_id": patient_id,
                    "treatment_plans": plans,
                    "plan_exercises": plan_exercises
                }
                    
            except Exception as e:
                print(f"Database error in debug treatment plans: {e}")
                print(f"Traceback: {traceback.format_exc()}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
        
        except Exception as e:
            print(f"Error in debug treatment plans: {e}")
            print(f"Traceback: {traceback.format_exc()}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )

    @app.post("/exercises/submissions/{submission_id}/mark-reviewed")
    async def mark_submission_reviewed(submission_id: int, request: Request):
        """Endpoint to mark a submission as reviewed by the therapist"""
        session_id = request.cookies.get("session_id")
        if not session_id:
            return JSONResponse(
                status_code=401,
                content={"detail": "Not authenticated"}
            )
        
        try:
            session_data = await get_redis_session(session_id)
            if not session_data:
                return JSONResponse(status_code=401, content={"detail": "Not authenticated"})
            
            therapist_id = session_data.get("user_id")
            
            db = get_Mysql_db()
            cursor = None
            
            try:
                cursor = db.cursor(dictionary=True)
                

                cursor.execute(
                    "SELECT id FROM Therapists WHERE id = %s",
                    (therapist_id,)
                )
                therapist = cursor.fetchone()
                
                if not therapist:
                    return JSONResponse(status_code=403, content={"detail": "Not authorized"})
                

                cursor.execute(
                    """
                    SELECT evs.submission_id, evs.status 
                    FROM ExerciseVideoSubmissions evs
                    JOIN Patients p ON evs.patient_id = p.patient_id
                    WHERE evs.submission_id = %s AND p.therapist_id = %s
                    """,
                    (submission_id, therapist_id)
                )
                
                submission = cursor.fetchone()
                if not submission:
                    return JSONResponse(
                        status_code=404, 
                        content={"detail": "Submission not found or not authorized to access"}
                    )
                

                cursor.execute(
                    """
                    UPDATE ExerciseVideoSubmissions 
                    SET status = 'Reviewed'
                    WHERE submission_id = %s AND status = 'Pending'
                    """,
                    (submission_id,)
                )
                
                db.commit()
                
                return JSONResponse(
                    status_code=200,
                    content={"status": "success", "message": "Submission marked as reviewed"}
                )
                
            except Exception as e:
                if db:
                    db.rollback()
                print(f"Database error in mark submission reviewed: {e}")
                return JSONResponse(
                    status_code=500,
                    content={"detail": f"Database error: {str(e)}"}
                )
            finally:
                if cursor:
                    cursor.close()
                if db:
                    db.close()
                    
        except Exception as e:
            print(f"Error in mark submission reviewed: {e}")
            return JSONResponse(
                status_code=500,
                content={"detail": f"Server error: {str(e)}"}
            )
        

    if __name__ == "__main__":
        base_url = getIP()
        uvicorn.run(app, host="0.0.0.0", port=8000, timeout_keep_alive=300)