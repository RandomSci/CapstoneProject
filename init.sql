-- Adminer 5.2.1 MySQL 8.0.41 dump

SET NAMES utf8;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `AppointmentRequests`;
CREATE TABLE `AppointmentRequests` (
  `request_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `therapist_id` int NOT NULL,
  `appointment_date` date NOT NULL,
  `appointment_time` time NOT NULL,
  `duration` int DEFAULT '60',
  `notes` text,
  `status` enum('Pending','Approved','Rejected') DEFAULT 'Pending',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`request_id`),
  KEY `idx_request_status` (`status`),
  KEY `idx_therapist_requests` (`therapist_id`,`status`),
  CONSTRAINT `AppointmentRequests_ibfk_1` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


DROP TABLE IF EXISTS `Appointments`;
CREATE TABLE `Appointments` (
  `appointment_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `therapist_id` int NOT NULL,
  `appointment_date` date NOT NULL,
  `appointment_time` time NOT NULL,
  `duration` int DEFAULT '60',
  `status` enum('Scheduled','Completed','Cancelled','No-Show') COLLATE utf8mb4_general_ci DEFAULT 'Scheduled',
  `notes` text COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`appointment_id`),
  KEY `patient_id` (`patient_id`),
  KEY `therapist_id` (`therapist_id`),
  CONSTRAINT `Appointments_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `Appointments_ibfk_2` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Appointments` (`appointment_id`, `patient_id`, `therapist_id`, `appointment_date`, `appointment_time`, `duration`, `status`, `notes`, `created_at`, `updated_at`) VALUES
(3,	5,	10,	'2025-04-15',	'11:00:00',	60,	'Scheduled',	'Type: Regular Session\nNotes: I have neck problems\nInsurance: Red ribbon\nMember ID: 10481',	'2025-04-16 03:59:45',	'2025-04-16 03:59:45');

DROP TABLE IF EXISTS `ExerciseCategories`;
CREATE TABLE `ExerciseCategories` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `description` text COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `ExerciseCategories` (`category_id`, `name`, `description`) VALUES
(1,	'Lower Extremity',	'Exercises focusing on hip, knee, ankle and foot rehabilitation'),
(2,	'Upper Extremity',	'Exercises for shoulder, elbow, wrist and hand rehabilitation'),
(3,	'Spine',	'Exercises for cervical, thoracic and lumbar spine rehabilitation'),
(4,	'Balance',	'Exercises to improve stability and reduce fall risk'),
(5,	'Core Strengthening',	'Exercises targeting abdominal and back muscles'),
(6,	'Functional Training',	'Activities that mimic daily living and work tasks'),
(7,	'Post-Surgical',	'Rehabilitation protocols following surgical procedures'),
(8,	'Sports Rehabilitation',	'Specialized exercises for athletic recovery');

DROP TABLE IF EXISTS `Exercises`;
CREATE TABLE `Exercises` (
  `exercise_id` int NOT NULL AUTO_INCREMENT,
  `therapist_id` int DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `description` text COLLATE utf8mb4_general_ci,
  `video_url` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `video_type` enum('youtube','upload','none') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `video_size` bigint DEFAULT NULL,
  `video_filename` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `duration` int DEFAULT NULL,
  `difficulty` enum('Beginner','Intermediate','Advanced') COLLATE utf8mb4_general_ci DEFAULT NULL,
  `instructions` text COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`exercise_id`),
  KEY `therapist_id` (`therapist_id`),
  KEY `category_id` (`category_id`),
  KEY `idx_category` (`category_id`),
  CONSTRAINT `Exercises_ibfk_1` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE,
  CONSTRAINT `Exercises_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `ExerciseCategories` (`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Exercises` (`exercise_id`, `therapist_id`, `category_id`, `name`, `description`, `video_url`, `video_type`, `video_size`, `video_filename`, `duration`, `difficulty`, `instructions`, `created_at`, `updated_at`) VALUES
(12,	NULL,	4,	'Knee Grow',	'Improve your lifestyle',	'https://www.youtube.com/watch?v=dQw4w9WgXcQ',	'youtube',	NULL,	NULL,	60,	'Advanced',	'Hold your knees and spin while singing never gonna give you up',	'2025-04-16 08:26:46',	'2025-04-16 13:29:35'),
(14,	NULL,	2,	'Push up',	'Push up while listening to this to make sure you stay motivated',	'https://www.youtube.com/watch?v=cvaIgq5j2Q8',	'youtube',	NULL,	NULL,	10,	'Beginner',	'Hold the mic and sign to your heart\'s content while dancing',	'2025-04-16 08:41:52',	'2025-04-16 08:44:26');

DROP TABLE IF EXISTS `Messages`;
CREATE TABLE `Messages` (
  `message_id` int NOT NULL AUTO_INCREMENT,
  `sender_id` int NOT NULL,
  `sender_type` enum('therapist','patient','user') COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'therapist',
  `recipient_id` int NOT NULL,
  `recipient_type` enum('therapist','patient','user') COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'therapist',
  `subject` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `content` text COLLATE utf8mb4_general_ci NOT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `recipient_id` (`recipient_id`,`is_read`),
  KEY `created_at` (`created_at`),
  KEY `idx_messages_sender` (`sender_id`,`sender_type`),
  KEY `idx_messages_recipient` (`recipient_id`,`recipient_type`),
  KEY `idx_messages_read_status` (`is_read`),
  CONSTRAINT `Messages_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE,
  CONSTRAINT `Messages_ibfk_2` FOREIGN KEY (`recipient_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Messages` (`message_id`, `sender_id`, `sender_type`, `recipient_id`, `recipient_type`, `subject`, `content`, `is_read`, `created_at`) VALUES
(11,	11,	'therapist',	10,	'therapist',	'Greetings',	'Hello!\r\n',	1,	'2025-04-10 13:23:54'),
(13,	10,	'therapist',	11,	'therapist',	'Re: Greetings',	'Hello din po!',	1,	'2025-04-10 13:24:32'),
(14,	11,	'therapist',	10,	'therapist',	'Re: Greetings',	'Kamusta?',	1,	'2025-04-11 02:42:07'),
(18,	13,	'therapist',	10,	'therapist',	'Greetings',	'Hello!',	0,	'2025-04-16 03:47:20');

DROP TABLE IF EXISTS `PatientExerciseProgress`;
CREATE TABLE `PatientExerciseProgress` (
  `progress_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `plan_exercise_id` int NOT NULL,
  `completion_date` date NOT NULL,
  `sets_completed` int DEFAULT NULL,
  `repetitions_completed` int DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `pain_level` int DEFAULT NULL,
  `difficulty_level` int DEFAULT NULL,
  `notes` text COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`progress_id`),
  KEY `patient_id` (`patient_id`),
  KEY `plan_exercise_id` (`plan_exercise_id`),
  CONSTRAINT `PatientExerciseProgress_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `PatientExerciseProgress_ibfk_2` FOREIGN KEY (`plan_exercise_id`) REFERENCES `TreatmentPlanExercises` (`plan_exercise_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `PatientMetrics`;
CREATE TABLE `PatientMetrics` (
  `metric_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `therapist_id` int NOT NULL,
  `measurement_date` date NOT NULL,
  `adherence_rate` decimal(5,2) DEFAULT NULL,
  `pain_level` int DEFAULT NULL,
  `functionality_score` int DEFAULT NULL,
  `recovery_progress` decimal(5,2) DEFAULT NULL,
  `notes` text COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`metric_id`),
  KEY `patient_id` (`patient_id`),
  KEY `therapist_id` (`therapist_id`),
  CONSTRAINT `PatientMetrics_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `PatientMetrics_ibfk_2` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `PatientNotes`;
CREATE TABLE `PatientNotes` (
  `note_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `therapist_id` int NOT NULL,
  `appointment_id` int DEFAULT NULL,
  `note_text` text COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`note_id`),
  KEY `patient_id` (`patient_id`),
  KEY `therapist_id` (`therapist_id`),
  KEY `appointment_id` (`appointment_id`),
  CONSTRAINT `PatientNotes_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `PatientNotes_ibfk_2` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE,
  CONSTRAINT `PatientNotes_ibfk_3` FOREIGN KEY (`appointment_id`) REFERENCES `Appointments` (`appointment_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `Patients`;
CREATE TABLE `Patients` (
  `patient_id` int NOT NULL AUTO_INCREMENT,
  `therapist_id` int NOT NULL,
  `first_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `last_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `phone` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `address` text COLLATE utf8mb4_general_ci,
  `diagnosis` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Active','Inactive','At Risk') COLLATE utf8mb4_general_ci DEFAULT 'Active',
  `notes` text COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`patient_id`),
  UNIQUE KEY `email` (`email`),
  KEY `therapist_id` (`therapist_id`),
  CONSTRAINT `Patients_ibfk_1` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Patients` (`patient_id`, `therapist_id`, `first_name`, `last_name`, `email`, `phone`, `date_of_birth`, `address`, `diagnosis`, `status`, `notes`, `created_at`, `updated_at`) VALUES
(5,	10,	'111',	'',	'111@gmail.com',	NULL,	NULL,	NULL,	NULL,	'Active',	NULL,	'2025-04-16 03:59:45',	'2025-04-16 03:59:45');

DROP TABLE IF EXISTS `Reviews`;
CREATE TABLE `Reviews` (
  `review_id` int NOT NULL AUTO_INCREMENT,
  `therapist_id` int NOT NULL,
  `patient_id` int NOT NULL,
  `rating` float NOT NULL,
  `comment` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  KEY `patient_id` (`patient_id`),
  KEY `idx_review_therapist` (`therapist_id`),
  CONSTRAINT `Reviews_ibfk_1` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`),
  CONSTRAINT `Reviews_ibfk_2` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


DELIMITER ;;

CREATE TRIGGER `after_review_insert` AFTER INSERT ON `Reviews` FOR EACH ROW
BEGIN
    UPDATE Therapists
    SET rating = (SELECT AVG(rating) FROM Reviews WHERE therapist_id = NEW.therapist_id),
        review_count = (SELECT COUNT(*) FROM Reviews WHERE therapist_id = NEW.therapist_id)
    WHERE id = NEW.therapist_id;
END;;

CREATE TRIGGER `after_review_update` AFTER UPDATE ON `Reviews` FOR EACH ROW
BEGIN
    UPDATE Therapists
    SET rating = (SELECT AVG(rating) FROM Reviews WHERE therapist_id = NEW.therapist_id),
        review_count = (SELECT COUNT(*) FROM Reviews WHERE therapist_id = NEW.therapist_id)
    WHERE id = NEW.therapist_id;
END;;

CREATE TRIGGER `after_review_delete` AFTER DELETE ON `Reviews` FOR EACH ROW
BEGIN
    UPDATE Therapists
    SET rating = COALESCE((SELECT AVG(rating) FROM Reviews WHERE therapist_id = OLD.therapist_id), 0),
        review_count = (SELECT COUNT(*) FROM Reviews WHERE therapist_id = OLD.therapist_id)
    WHERE id = OLD.therapist_id;
END;;

DELIMITER ;

DROP TABLE IF EXISTS `Therapists`;
CREATE TABLE `Therapists` (
  `id` int NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `last_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `company_email` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `profile_image` varchar(255) COLLATE utf8mb4_general_ci DEFAULT 'avatar-1.jpg',
  `bio` text COLLATE utf8mb4_general_ci,
  `experience_years` int DEFAULT '0',
  `specialties` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `education` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `languages` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `address` text COLLATE utf8mb4_general_ci,
  `rating` float DEFAULT '0',
  `review_count` int DEFAULT '0',
  `is_accepting_new_patients` tinyint(1) DEFAULT '1',
  `average_session_length` int DEFAULT '60',
  PRIMARY KEY (`id`),
  UNIQUE KEY `company_email` (`company_email`),
  KEY `idx_therapist_rating` (`rating`),
  CONSTRAINT `Therapists_chk_1` CHECK (json_valid(`specialties`)),
  CONSTRAINT `Therapists_chk_2` CHECK (json_valid(`education`)),
  CONSTRAINT `Therapists_chk_3` CHECK (json_valid(`languages`))
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Therapists` (`id`, `first_name`, `last_name`, `company_email`, `password`, `profile_image`, `bio`, `experience_years`, `specialties`, `education`, `languages`, `address`, `rating`, `review_count`, `is_accepting_new_patients`, `average_session_length`) VALUES
(10,	'Mariah',	'Yalong',	'Yalong@gmail.com',	'$2b$12$jsQuPBFFg0x1cwPXoyJbgeefWq4usxclTKfArvi5tiuNiwlH8fvle',	'therapist_10_1744774979.jpg',	'Therapy expert',	10,	'[\"Orthopedic Physical Therapy\", \"Neurological Physical Therapy\", \"Cardiovascular & Pulmonary Physical Therapy\", \"Pediatric Physical Therapy\", \"Geriatric Physical Therapy\", \"Sports Physical Therapy\", \"Women\'s Health Physical Therapy\", \"Manual Therapy\", \"Vestibular Rehabilitation\", \"Post-Surgical Rehabilitation\", \"Pain Management\"]',	'[\"Harvard University\"]',	'[\"English\", \"Spanish\", \"French\", \"German\", \"Italian\", \"Portuguese\"]',	'Antipolo',	5,	98,	1,	60),
(11,	'Selwyn',	'Jayme',	'jayme@gmail.com',	'$2b$12$aiW6j9qcPt5285m7hNcLV.TOGceFyqysucrJGx1OwMNzk0sQbwpLi',	'therapist_11_1744746361.jpg',	'',	0,	'[]',	'[]',	'[]',	'',	0,	0,	1,	60),
(13,	'Thera',	'Pist',	'Therapist@gmail.com',	'$2b$12$Ep6F82M4bPSTC97OPoCAd.NJLUIXZ7ZF97fl.COePctMrtgKPh/IW',	'therapist_13_1744775202.png',	'',	0,	'[]',	'[]',	'[]',	'',	0,	0,	1,	60);

DROP TABLE IF EXISTS `TreatmentPlanExercises`;
CREATE TABLE `TreatmentPlanExercises` (
  `plan_exercise_id` int NOT NULL AUTO_INCREMENT,
  `plan_id` int NOT NULL,
  `exercise_id` int NOT NULL,
  `sets` int DEFAULT '1',
  `repetitions` int DEFAULT NULL,
  `frequency` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `duration` int DEFAULT NULL,
  `notes` text COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`plan_exercise_id`),
  KEY `plan_id` (`plan_id`),
  KEY `exercise_id` (`exercise_id`),
  CONSTRAINT `TreatmentPlanExercises_ibfk_1` FOREIGN KEY (`plan_id`) REFERENCES `TreatmentPlans` (`plan_id`) ON DELETE CASCADE,
  CONSTRAINT `TreatmentPlanExercises_ibfk_2` FOREIGN KEY (`exercise_id`) REFERENCES `Exercises` (`exercise_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `TreatmentPlanExercises` (`plan_exercise_id`, `plan_id`, `exercise_id`, `sets`, `repetitions`, `frequency`, `duration`, `notes`, `created_at`) VALUES
(2,	2,	12,	17,	10,	'Daily',	10,	'sadsadas',	'2025-04-16 12:56:04'),
(5,	2,	14,	7,	10,	'Every other day',	10,	'',	'2025-04-16 13:14:17');

DROP TABLE IF EXISTS `TreatmentPlans`;
CREATE TABLE `TreatmentPlans` (
  `plan_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `therapist_id` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `description` text COLLATE utf8mb4_general_ci,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `status` enum('Active','Completed','Cancelled') COLLATE utf8mb4_general_ci DEFAULT 'Active',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`plan_id`),
  KEY `patient_id` (`patient_id`),
  KEY `therapist_id` (`therapist_id`),
  CONSTRAINT `TreatmentPlans_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `TreatmentPlans_ibfk_2` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `TreatmentPlans` (`plan_id`, `patient_id`, `therapist_id`, `name`, `description`, `start_date`, `end_date`, `status`, `created_at`, `updated_at`) VALUES
(2,	5,	10,	'Sample Plan',	'Just an example of course',	'2025-04-16',	'2025-06-11',	'Cancelled',	'2025-04-16 12:56:03',	'2025-04-16 13:23:00');

DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `feedback_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `message` text COLLATE utf8mb4_general_ci NOT NULL,
  `rating` int DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feedback_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `feedback_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `feedback_chk_1` CHECK ((`rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `profile_pic` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `users` (`user_id`, `username`, `email`, `password_hash`, `profile_pic`, `created_at`, `updated_at`) VALUES
(38,	'111',	'111@gmail.com',	'$2b$12$CwEvo2MRR954ZXkJugnjfezcFDiPsV4k2wFD7ygWQJu1mgfou9Xom',	NULL,	'2025-04-10 22:57:28',	'2025-04-10 22:57:28'),
(40,	'222',	'222@gmail.com',	'$2b$12$Hsa7oCHXJCd4yNGVXw./MeuVrx0OhxLsAEX33Bh/3Q8DRvkKi0U5G',	NULL,	'2025-04-15 19:45:21',	'2025-04-15 19:45:21');

-- 2025-04-16 13:37:58 UTC
