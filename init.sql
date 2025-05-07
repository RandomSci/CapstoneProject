-- Adminer 5.2.1 MySQL 8.0.41 dump

SET NAMES utf8;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `Appointments`;
CREATE TABLE `Appointments` (
  `appointment_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `therapist_id` int NOT NULL,
  `appointment_date` date NOT NULL,
  `appointment_time` time NOT NULL,
  `duration` int DEFAULT '60',
  `status` enum('Scheduled','Completed','Cancelled','No-Show') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'Scheduled',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`appointment_id`),
  KEY `patient_id` (`patient_id`),
  KEY `therapist_id` (`therapist_id`),
  CONSTRAINT `Appointments_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `Appointments_ibfk_2` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Appointments` (`appointment_id`, `patient_id`, `therapist_id`, `appointment_date`, `appointment_time`, `duration`, `status`, `notes`, `created_at`, `updated_at`) VALUES
(54,	26,	17,	'2025-05-04',	'11:00:00',	60,	'Completed',	'Type: video\n',	'2025-05-03 16:51:24',	'2025-05-03 16:53:21');

DROP TABLE IF EXISTS `ExerciseCategories`;
CREATE TABLE `ExerciseCategories` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
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

DROP TABLE IF EXISTS `ExerciseVideoSubmissions`;
CREATE TABLE `ExerciseVideoSubmissions` (
  `submission_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `exercise_id` int NOT NULL,
  `treatment_plan_id` int NOT NULL,
  `video_url` varchar(512) NOT NULL,
  `submission_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `notes` text,
  `status` enum('Pending','Reviewed','Feedback Provided') DEFAULT 'Pending',
  `therapist_feedback` text,
  `feedback_rating` enum('Poor','Needs Improvement','Good','Excellent') DEFAULT NULL,
  `feedback_date` timestamp NULL DEFAULT NULL,
  `file_size` bigint DEFAULT NULL COMMENT 'Size of the video file in bytes',
  `analysis_data` json DEFAULT NULL,
  `analysis_date` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`submission_id`),
  KEY `patient_id` (`patient_id`),
  KEY `exercise_id` (`exercise_id`),
  KEY `treatment_plan_id` (`treatment_plan_id`),
  KEY `status` (`status`),
  CONSTRAINT `ExerciseVideoSubmissions_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `ExerciseVideoSubmissions_ibfk_2` FOREIGN KEY (`exercise_id`) REFERENCES `Exercises` (`exercise_id`) ON DELETE CASCADE,
  CONSTRAINT `ExerciseVideoSubmissions_ibfk_3` FOREIGN KEY (`treatment_plan_id`) REFERENCES `TreatmentPlans` (`plan_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


DROP TABLE IF EXISTS `Exercises`;
CREATE TABLE `Exercises` (
  `exercise_id` int NOT NULL AUTO_INCREMENT,
  `therapist_id` int DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `video_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `video_type` enum('youtube','upload','none') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `video_size` bigint DEFAULT NULL,
  `video_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `duration` int DEFAULT NULL,
  `difficulty` enum('Beginner','Intermediate','Advanced') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `instructions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`exercise_id`),
  KEY `therapist_id` (`therapist_id`),
  KEY `category_id` (`category_id`),
  KEY `idx_category` (`category_id`),
  CONSTRAINT `Exercises_ibfk_1` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE,
  CONSTRAINT `Exercises_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `ExerciseCategories` (`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=94 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Exercises` (`exercise_id`, `therapist_id`, `category_id`, `name`, `description`, `video_url`, `video_type`, `video_size`, `video_filename`, `duration`, `difficulty`, `instructions`, `created_at`, `updated_at`) VALUES
(16,	NULL,	1,	'20 Hip Thrusts',	'EQUIPMENT NEEDED : COUCH',	'https://www.youtube.com/watch?v=E0pOOfQr0zI',	'youtube',	NULL,	NULL,	5,	'Beginner',	'Place feet shoulder-width apart on the floor, back on the couch.\r\nHinge at the hips, moving them toward the floor\r\nUsing your glutes, bring your hips up toward the ceiling again.',	'2025-04-20 12:12:01',	'2025-04-20 12:54:07'),
(17,	NULL,	2,	'20 Shoulder Taps',	'EQUIPMENT NEEDED : COUCH',	'https://www.youtube.com/watch?v=6-8E4Nirh9s',	'youtube',	NULL,	NULL,	5,	'Beginner',	'- Pushup position.\r\n- Touch R hand to L shoulder, back to starting position.\r\n- Switch and repeat.\r\n\r\nREST FOR 1 - 2 MINS AFTER COMPLETING\r\nEXERCISES 1 - 5\r\nREPEAT 3 - 5 TIMES',	'2025-04-20 12:22:24',	'2025-04-20 12:54:28'),
(18,	NULL,	5,	'15 Supermans',	'Lay on stomach with arms outstretched in front.',	'https://www.youtube.com/watch?v=6-8E4Nirh9s',	'youtube',	NULL,	NULL,	2,	'Beginner',	'- Lay on stomach with arms outstretched in front.\r\n- Simultaneously raise arms, trunk, and lower body.\r\n- Lower back down and repeat.',	'2025-04-20 12:27:49',	'2025-04-23 10:26:12'),
(19,	NULL,	5,	'10 Bear Crawl Burpees',	'Start standing, walk hands out to pushup position, pushup.',	'https://www.youtube.com/watch?v=6-8E4Nirh9s',	'youtube',	NULL,	NULL,	5,	'Beginner',	'- Start standing, walk hands out to pushup position, pushup.\r\n- Walk hands back to feet.\r\n- Jump.',	'2025-04-20 12:56:40',	'2025-04-23 10:26:32'),
(20,	NULL,	1,	'15 Leg Lowers',	'Lay down on floor with head and shoulders resting.',	'https://www.youtube.com/watch?v=6-8E4Nirh9s',	'youtube',	NULL,	NULL,	10,	'Beginner',	'- Lay down on floor with head and shoulders resting.\r\n- Legs are straight and positioned up toward the ceiling (90 degree angle).\r\n- Lower legs down to hover 2\" above the ground, Raise back to starting position.',	'2025-04-20 13:05:58',	'2025-04-23 10:27:02'),
(21,	NULL,	1,	'15 Weighted Couch Squats',	'EQUIPMENT NEEDED :\r\n    - C O U C H\r\n    - BACKPACK W / B O O K S  INSIDE\r\n     -  2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=6-8E4Nirh9s',	'youtube',	NULL,	NULL,	10,	'Beginner',	'- Put backpack on, sit on edge of couch, feet slightly wider than shoulders.\r\n- Driving through the heels, stand up from seated position.\r\n- For added challenge: turn it into a jump!',	'2025-04-20 13:09:40',	'2025-04-23 10:27:18'),
(22,	NULL,	4,	'20 Overhead Presses (w/ weighted objects)',	'- Hold objects in hands, Raise arms to the sides, parallel with floor, bending 90',	'https://www.youtube.com/watch?v=oeXF794hDmQ',	'youtube',	NULL,	NULL,	5,	'Beginner',	'- Hold objects in hands, Raise arms to the sides, parallel with floor, bending 90\r\ndegrees at the elbow (like a football goal post).\r\n- Raise objects straight above and back to starting position',	'2025-04-23 10:30:07',	'2025-04-26 09:57:53'),
(23,	NULL,	1,	'20 Bent Over Rows (w/ weighted objects)',	'- Stand upright, feet shoulder-width apart. Bend trunk over feet, almost parallel with  the\r\nfloor, slight bend in knees',	'https://www.youtube.com/watch?v=oeXF794hDmQ',	'youtube',	NULL,	NULL,	10,	'Beginner',	'- Stand upright, feet shoulder-width apart. Bend trunk over feet, almost parallel w/ the\r\nfloor, slight bend in knees.\r\n- Hands outstretched toward floor, palms facing each other. Pull elbows back to be by\r\nside, squeezing the back. Return to starting position.',	'2025-04-23 10:31:50',	'2025-04-23 10:31:50'),
(24,	NULL,	2,	'45 secs Mountainclimbers + Pushups',	'- Pushup position, start running in place, bringing knees towards arms (x10).',	'https://www.youtube.com/watch?v=oeXF794hDmQ',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Pushup position, start running in place, bringing knees towards arms (x10).\r\n- 5 pushups.\r\n- Repeat for the full 45 secs.',	'2025-04-23 10:33:22',	'2025-04-23 10:33:22'),
(25,	NULL,	5,	'15 Full Body Crunches',	'Lay down on floor with arms and feet outstretched.',	'https://www.youtube.com/watch?v=oeXF794hDmQ',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Lay down on floor with arms and feet outstretched.\r\n- Simultaneously bring knees and arms together, forming a little ball.\r\n- Return to starting position and repeat.',	'2025-04-23 10:34:23',	'2025-04-23 10:34:23'),
(26,	NULL,	1,	'10 Step, Step, Jump',	'Start sitting on knees.',	'https://www.youtube.com/watch?v=oeXF794hDmQ',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Start sitting on knees.\r\n- Bring R foot up to a lunge, Next L foot up to be in a squat. Jump!\r\n- One leg at a time, return to starting position.',	'2025-04-23 10:35:43',	'2025-04-23 10:35:43'),
(27,	NULL,	5,	'15 Dips',	'Sit just past edge of coffee table, hands gripping edge.',	NULL,	'none',	NULL,	NULL,	1,	'Intermediate',	'- Sit just past edge of coffee table, hands gripping edge.\r\n- Lower yourself down so your arms reach a 90 degree angle.\r\n- Extend arms back to starting position.',	'2025-04-23 10:36:42',	'2025-04-23 10:36:42'),
(28,	NULL,	1,	'12 Bulgarian Split Squats (each leg)',	'Stand in front of couch. Place top of one foot on the couch.',	'https://www.youtube.com/watch?v=oeXF794hDmQ',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Stand in front of couch. Place top of one foot on the couch.\r\n- With body weight directed over the heel of the front foot, lunge down, past\r\nparallel. Return to starting position and repeat.',	'2025-04-23 10:37:20',	'2025-04-23 10:37:20'),
(29,	NULL,	6,	'45 secs Shoulder External rotations',	'Position arms at a 90 degree angle with elbows by your side and palms',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Position arms at a 90 degree angle with elbows by your side and palms\r\nfacing each other.\r\n- Rotate both forearms so hands now face front wall, keeping elbows at side.',	'2025-04-23 10:44:29',	'2025-04-23 10:44:29'),
(30,	NULL,	5,	'20 Plank twists',	'Start in pushup position. Bring L arm up toward ceiling, body facing L wall',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Start in pushup position. Bring L arm up toward ceiling, body facing L wall\r\n(one-armed side plank). Return to pushup position.\r\n- Repeat with R side',	'2025-04-23 10:45:24',	'2025-04-23 10:45:24'),
(31,	NULL,	4,	'16 Sprinter Pulls',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Start in a lunge, bring back foot forward into the air (like you\'re mid-run).\r\nSwing arms to help with the motion\r\n- Return same leg back to the starting position. Repeat x8 then switch legs.',	'2025-04-23 10:46:57',	'2025-04-23 10:46:57'),
(32,	NULL,	2,	'20 Romanian Deadlifts',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	NULL,	'none',	NULL,	NULL,	1,	'Intermediate',	'- Stand holding your weighted objects on the front of your thighs.\r\n- Keep your legs relatively straight and hinge back at the hips while you lower\r\nthe weights down your legs till you feel a stretch. Slowly reverse the movement.',	'2025-04-23 10:47:35',	'2025-04-23 10:47:35'),
(33,	NULL,	2,	'10 Tricep Pushups',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	NULL,	'none',	NULL,	NULL,	1,	'Intermediate',	'- Angle elbows towards the back of the room. Hands should be under your\r\nshoulders and elbows graze your sides on the way down.\r\n- Modification: do knee tricep pushups.',	'2025-04-23 10:48:26',	'2025-04-23 10:48:57'),
(34,	NULL,	5,	'20 Lower Ab \"U\'s\"',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Lay on back with hands using couch for support.\r\n- With legs straight, go back and forth forming a \"U\" with your feet, avoiding\r\nthe ground at the bottom and keeping your back flat against the ground.',	'2025-04-23 10:50:11',	'2025-04-23 10:50:11'),
(35,	NULL,	1,	'20 Three-Legged Dog Swings',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Start in Downward Dog. Raise L leg toward ceiling (3-legged dog).\r\n- In one motion shift weight forward while bringing L knee between shoulders.\r\n- Return to starting position and repeat on opposite side.',	'2025-04-23 10:51:07',	'2025-04-23 10:51:07'),
(36,	NULL,	1,	'16 180-Degree Squat jumps',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Squat jump, and while mid-air jump 180 degrees, landing in another squat.',	'2025-04-23 10:51:49',	'2025-04-23 10:51:49'),
(37,	NULL,	1,	'20 Chest Fly\'s',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	NULL,	'none',	NULL,	NULL,	1,	'Intermediate',	'- Lay down on ground w/ weighted objects outstretched in front of your chest, barely\r\ntouching.\r\n- With arms SLIGHTLY bent, lower arms towards the ground. Return to top, squeezing\r\nchest as hands come closer together.',	'2025-04-23 10:52:36',	'2025-04-23 10:52:36'),
(38,	NULL,	1,	'20 Chest Fly\'s',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Lay down on ground w/ weighted objects outstretched in front of your chest, barely\r\ntouching.\r\n- With arms SLIGHTLY bent, lower arms towards the ground. Return to top, squeezing\r\nchest as hands come closer together.',	'2025-04-23 10:52:51',	'2025-04-23 10:52:51'),
(39,	NULL,	2,	'20 Side Lying Arm Rotations',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Lay down with L arm holding weighted object outstretched like above exercise.\r\n- Rotate body onto R side while keeping L arm in the same position.\r\n- Return L side of body back to the floor and repeat on the other side.',	'2025-04-23 10:53:46',	'2025-04-23 10:53:46'),
(40,	NULL,	5,	'20 Lunges with Glute Kickback',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	NULL,	'none',	NULL,	NULL,	1,	'Intermediate',	'- Lunge with R leg in front.\r\n- Step forward onto R leg and kick L leg behind, squeezing the glute.\r\n- 10 on each side.',	'2025-04-23 10:54:27',	'2025-04-23 10:54:27'),
(41,	NULL,	5,	'20 Side Plank Crunches',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Get into a side plank position, preferably on you hand.\r\n- Outstretch free arm above head and then side crunch, bending knee and\r\nelbow together.',	'2025-04-23 10:55:34',	'2025-04-23 10:55:34'),
(42,	NULL,	4,	'20 Switch Lunges',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Lunge with R leg forward, jump and switch so L leg in front. Repeat.',	'2025-04-23 10:56:24',	'2025-04-23 10:56:24'),
(43,	NULL,	4,	'20 Elevated Glute Bridges',	'',	NULL,	'none',	NULL,	NULL,	1,	'Intermediate',	'- Lay on floor with feet on couch. Drive hips towards ceiling w/ weight in heels.',	'2025-04-23 10:56:59',	'2025-04-23 10:56:59'),
(44,	NULL,	4,	'20 Elevated Glute Bridges',	'',	'https://www.youtube.com/watch?v=hTaYc3Rg-ZA',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Lay on floor with feet on couch. Drive hips towards ceiling w/ weight in heels.',	'2025-04-23 10:57:29',	'2025-04-23 10:57:29'),
(45,	NULL,	1,	'20 Lateral Raises',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=j08epXbBlyY',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Hold weighted objects in both hands at your side.\r\n- Raise arms simultaneously, in the same plane as your body, becoming parallel with the\r\nfloor. Maintain a small bend in the elbow. Lower back to your sides.',	'2025-04-23 10:58:32',	'2025-04-23 10:58:32'),
(46,	NULL,	5,	'20 Hex Presses',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=j08epXbBlyY',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Lay down on the floor with knees bent, feet on the floor. Arms outstretched, holding\r\nweighted objects.\r\n- Bend objects down to touch the chest and back up to starting position, maintaining\r\ncontact between the objects the whole time.',	'2025-04-23 10:59:17',	'2025-04-23 10:59:17'),
(47,	NULL,	3,	'20 Back Extensions',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=j08epXbBlyY',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Lay on floor, belly on the ground with hands behind your ears.\r\n- Using your back muscles, raise head and upper body off the ground, keeping\r\npelvis and legs on the ground. Lower back to the ground.',	'2025-04-23 10:59:58',	'2025-04-23 10:59:58'),
(48,	NULL,	5,	'40 Bicycle Crunches',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=j08epXbBlyY',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Lay on back with hands behind your ears. Keep L leg straight, R leg bent.\r\n- Touch L elbow to R knee and then switch, touching R elbow to L knee.',	'2025-04-23 11:00:46',	'2025-04-23 11:00:46'),
(49,	NULL,	1,	'20 Curtsy Lunges ',	'Engaging glutes, quadriceps, and hamstrings, the curtsy lunge enhances leg strength, flexibility, and core stability.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'Start: Stand with feet hip-width apart, hands on your hips. \r\nStep: Step back with one leg, crossing it behind the other leg, as if doing a curtsy. \r\nLower: Bend both knees, keeping the front knee over the ankle and the back knee close to the floor. \r\nReturn: Push through the front foot to return to the starting position. \r\nRepeat: Alternate sides and repeat for the desired number of reps and sets. ',	'2025-04-23 11:02:40',	'2025-04-23 11:02:40'),
(50,	NULL,	1,	'20 Curtsy Lunges ',	'Engaging glutes, quadriceps, and hamstrings, the curtsy lunge enhances leg strength, flexibility, and core stability.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'Start: Stand with feet hip-width apart, hands on your hips. \r\nStep: Step back with one leg, crossing it behind the other leg, as if doing a curtsy. \r\nLower: Bend both knees, keeping the front knee over the ankle and the back knee close to the floor. \r\nReturn: Push through the front foot to return to the starting position. \r\nRepeat: Alternate sides and repeat for the desired number of reps and sets. ',	'2025-04-23 11:02:52',	'2025-04-23 11:02:52'),
(51,	NULL,	5,	'30 Jumping Jacks',	'Jumping jacks are a simple, effective full-body exercise that can be done anywhere with no equipment. It involves jumping while spreading your legs and raising your arms overhead, then returning to the starting position.',	NULL,	'none',	NULL,	NULL,	3,	'Beginner',	'Start: Stand with your feet together and arms at your sides. \r\nJump: Jump up and spread your legs wide, simultaneously bringing your arms up above your head. \r\nReturn: Jump back to the starting position, with feet together and arms at your sides. \r\nRepeat: Continue jumping jacks for the desired number of repetitions or duration.',	'2025-04-23 11:03:56',	'2025-04-23 11:03:56'),
(52,	NULL,	5,	'30 Jumping Jacks',	'Jumping jacks are a simple, effective full-body exercise that can be done anywhere with no equipment. It involves jumping while spreading your legs and raising your arms overhead, then returning to the starting position.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Beginner',	'Start: Stand with your feet together and arms at your sides. \r\nJump: Jump up and spread your legs wide, simultaneously bringing your arms up above your head. \r\nReturn: Jump back to the starting position, with feet together and arms at your sides. \r\nRepeat: Continue jumping jacks for the desired number of repetitions or duration.',	'2025-04-23 11:04:08',	'2025-04-23 11:04:08'),
(53,	NULL,	5,	'30 Planks with Object Slide',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Get into a plank position (on hands). Place 1 weighted object on floor by L hand.\r\n- Use R hand to slide object to R side. Repeat with L hand.',	'2025-04-23 11:05:32',	'2025-04-23 11:05:32'),
(54,	NULL,	1,	'15 Single Leg Hip Thrusts',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Stand up, holding weighted backpack.\r\n- Bend over so your trunk is almost parallel with the floor.\r\n- Keeping elbows by your side, bring backpack towards your stomach and then back',	'2025-04-23 11:06:24',	'2025-04-23 11:06:24'),
(55,	NULL,	1,	'20 Alternating Toe Taps',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Lay on back with hands outstretched above your head.\r\n- In one motion, touch your R hand to your L leg and bring back to lying position.\r\nRepeat with L hand to R leg.',	'2025-04-23 11:07:44',	'2025-04-23 11:07:44'),
(56,	NULL,	1,	'20 Full Body Sit-ups',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	NULL,	'none',	NULL,	NULL,	1,	'Intermediate',	'- Sit-ups, but keep your legs straight and try and touch your toes.',	'2025-04-23 11:08:38',	'2025-04-23 11:08:38'),
(57,	NULL,	1,	'20 Full Body Sit-ups',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	1,	'Intermediate',	'- Sit-ups, but keep your legs straight and try and touch your toes.',	'2025-04-23 11:08:52',	'2025-04-23 11:08:52'),
(58,	NULL,	1,	'20 Couch Pistol Squats',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Sit on edge of couch, with 1 foot on ground and other leg extended.\r\n- Stand up using only 1 leg.',	'2025-04-23 11:09:36',	'2025-04-23 11:09:36'),
(59,	NULL,	4,	'20 Sec Handstand',	'Use a free wall for support (don\'t be knocking over any pictures!).',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Beginner',	'- Use a free wall for support (don\'t be knocking over any pictures!).\r\n- Modification: put feet on couch and hands on floor',	'2025-04-23 11:11:16',	'2025-04-23 11:11:16'),
(60,	NULL,	4,	'20 Sec Handstand',	'Use a free wall for support (don\'t be knocking over any pictures!).',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Beginner',	'- Use a free wall for support (don\'t be knocking over any pictures!).\r\n- Modification: put feet on couch and hands on floor',	'2025-04-23 11:11:25',	'2025-04-23 11:11:25'),
(61,	NULL,	1,	'20 Towel Lat Squeeze',	'Lay on stomach on the floor, with arms outstretched in front, holding towel ends.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Lay on stomach on the floor, with arms outstretched in front, holding towel ends.\r\n- In one controlled motion, bring towel down behind your head, squeezing your lats.\r\nReturn to starting position and repeat.',	'2025-04-23 11:12:14',	'2025-04-23 11:12:14'),
(62,	NULL,	5,	'30 secs Side-to-Side Pushup Walks',	'Get into pushup position and perform a pushup.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Intermediate',	'- Get into pushup position and perform a pushup.\r\n- Use hands and feet to move 2 steps to R side.\r\n- Perform another pushup and then move 2 steps to L side.',	'2025-04-23 11:12:57',	'2025-04-23 11:12:57'),
(63,	NULL,	1,	'20 Bird Dogs',	'Complete 10 and then switch to R arm and L leg.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Get on all fours.\r\n- Simultaneously raise L arm in front and R leg in back. Return to all fours.\r\n- Complete 10 and then switch to R arm and L leg.',	'2025-04-23 11:14:00',	'2025-04-23 11:14:00'),
(64,	NULL,	2,	'10 Decline Couch Pushups',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Put your feet on the couch and your hands on the floor.',	'2025-04-23 11:15:20',	'2025-04-23 11:15:20'),
(65,	NULL,	4,	'20 Good Mornings',	'Stand on ground with hands behind your ears.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Stand on ground with hands behind your ears.\r\n- Slowly lean trunk forward while hinging back at the hips, keeping a straight back\r\nand relatively straight legs.\r\n- Once you feel a stretch, reverse the movement slowly back to standing and repeat.',	'2025-04-23 11:16:02',	'2025-04-23 11:16:02'),
(66,	NULL,	1,	'30 secs Hollow-Body Hold',	'Keeping only your butt on the ground, hold both your feet and upper body 6\" from\r\nthe ground, engaging your abs.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Get on the floor.\r\n- Keeping only your butt on the ground, hold both your feet and upper body 6\" from\r\nthe ground, engaging your abs.',	'2025-04-23 11:17:10',	'2025-04-23 11:17:10'),
(67,	NULL,	1,	'20 Couch Squat Jumps',	'Sit on the edge of the couch with feet on the floor.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Sit on the edge of the couch with feet on the floor.\r\n- Jump up, driving through the heels.\r\n- Return to the couch and repeat.',	'2025-04-23 11:18:31',	'2025-04-23 11:18:31'),
(68,	NULL,	5,	'20 Plank \"Down-Down-Up-Up\'s\"',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Get into a plank position (on hands).\r\n- Bring R elbow down, L elbow down (now in plank position on elbows).\r\n- Put R hand down, L hand down, returning to original plank. Repeat on L side.',	'2025-04-23 11:19:16',	'2025-04-23 11:19:16'),
(69,	NULL,	1,	'50 Lying Hip Abduction Raises',	'Lie on your side with legs outstretched.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Lie on your side with legs outstretched.\r\n- Raise the top foot several inches and return down. Repeat x25 and then\r\nswitch sides.',	'2025-04-23 11:20:30',	'2025-04-23 11:20:30'),
(70,	NULL,	1,	'50 Lying Hip Abduction Raises',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Intermediate',	'- Lie on your side with legs outstretched.\r\n- Raise top foot several inches and return back down. Repeat x25 and then\r\nswitch sides.',	'2025-04-23 11:21:26',	'2025-04-23 11:21:26'),
(71,	NULL,	1,	'50 Lying Hip Abduction Raises',	'EQUIPMENT NEEDED:\r\n- COUCH\r\n- 2 WEIGHTED OBJECTS',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Intermediate',	'- Lie on your side with legs outstretched.\r\n- Raise top foot several inches and return back down. Repeat x25 and then\r\nswitch sides.',	'2025-04-23 11:21:36',	'2025-04-23 11:21:36'),
(72,	NULL,	4,	'20 Dips',	' Use a coffee table to place your hands.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Beginner',	'- Use a coffee table to place your hands.',	'2025-04-23 11:22:49',	'2025-04-23 11:22:49'),
(73,	NULL,	5,	'40 Russian twists',	' Use a weighted object.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Use a weighted object.',	'2025-04-23 11:23:42',	'2025-04-23 11:23:42'),
(74,	NULL,	6,	'CIRCUIT 1',	'HIIT\r\nWorkout\r\n\r\nEQUIPMENT NEEDED:\r\n- NONE\r\n\r\n30 SECS ON:30 SECS REST\r\nOR 45 SECS ON:30 SEC REST',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Advanced',	'1. Squat jumps\r\n2. Bear crawl to pushup position and back\r\n3. Lunges with arm reach to the sky**\r\n4. Bicycle crunches\r\n\r\nRest for 1 min.\r\nRepeat whole circuit 3-4x before moving on to Circuit 2.',	'2025-04-23 11:26:19',	'2025-04-23 11:26:19'),
(75,	NULL,	6,	'CIRCUIT 2',	'HIIT\r\nWorkout\r\n\r\nEQUIPMENT NEEDED:\r\n- NONE\r\n\r\n30 SECS ON:30 SECS REST\r\nOR 45 SECS ON:30 SEC REST',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Advanced',	'1. Burpees\r\n2. R Curtsy lunge, sumo squat, L curtsy lunge, sumo squat**\r\n3. Plank twists\r\n- Pushup position, reach R hand to ceiling, back to floor, L hand to ceiling.\r\n4. Toe Taps\r\n- Lay on back, legs pointing towards ceiling (body 90 degree position).\r\nRest for 1 min.\r\nRepeat whole circuit 3-4x.',	'2025-04-23 11:26:58',	'2025-04-23 11:26:58'),
(76,	NULL,	6,	'CIRCUIT 1 B',	'HIIT\r\nWorkout number 2\r\n\r\nEQUIPMENT NEEDED:\r\n- NONE\r\n\r\n30 SECS ON:30 SECS REST\r\nOR 45 SECS ON:30 SEC REST',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Advanced',	'1. Couch jumps\r\n- Sit at edge of couch, feet on floor. Jump up and return to couch. Repeat.\r\n2. Elevated pushups\r\n- Perform pushups while your feet are on the couch.\r\n3. Single Leg Hip Thrusts\r\n- Use couch as support for your back.\r\n4. Sit-ups\r\n\r\nRest for 1 min.\r\nRepeat whole circuit 3-4x before moving on to Circuit 2.',	'2025-04-23 11:28:14',	'2025-04-23 11:28:14'),
(77,	NULL,	6,	'CIRCUIT 2 B',	'HIIT\r\nWorkout number 2\r\n\r\nEQUIPMENT NEEDED:\r\n- NONE\r\n\r\n30 SECS ON:30 SECS REST\r\nOR 45 SECS ON:30 SEC REST',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Advanced',	'1. Switch Jump Lunges\r\n2. Advanced Bird Dogs\r\n- Pushup position. Raise R arm and L leg. Then raise L arm and R leg. Repeat.\r\n3. Leg Lowers\r\n- Use couch for support of your hands.\r\n4. Down, Down, Up, Up\'s\r\n- Pushup position. Down to R elbow, Down to L elbow, Up onto R hand, Up\r\nonto L hand. Repeat.\r\nRest for 1 min.\r\nRepeat whole circuit 3-4x.',	'2025-04-23 11:28:57',	'2025-04-23 11:28:57'),
(78,	NULL,	6,	'Dumbbell Workout EXERCISE 1',	'Dumbbell\r\nWorkout\r\nNUMBER 1\r\n\r\nEQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Advanced',	'20 Squat & Press\r\n- Hold DBs in hands in front of your shoulders, elbows bent.\r\n- Perform a squat, and when you stand up, raise arms to ceiling.\r\n- Bring arms down and then go into you next squat.',	'2025-04-23 11:30:47',	'2025-04-23 11:30:47'),
(79,	NULL,	6,	'Dumbbell Workout EXERCISE 2',	'Dumbbell\r\nWorkout\r\nNUMBER 2\r\n\r\nEQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Advanced',	'20 Plank Rows\r\n- Get into a pushup position while holding DBs.\r\n- While keeping body steady bring R elbow back to be by your side and back\r\nto floor. Repeat on L side.',	'2025-04-23 11:32:07',	'2025-04-23 11:32:07'),
(80,	NULL,	6,	'20 DB Floor Press',	'Dumbbell\r\nWorkout\r\nNUMBER 2\r\n\r\nEQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Intermediate',	'- Lay down on ground holding DBs in hands outstretched in front of chest.\r\n- Perform a chest press, bringing elbows to lightly touch the ground and then\r\nback to starting position.',	'2025-04-23 11:33:41',	'2025-04-23 11:33:41'),
(81,	NULL,	2,	'20 Toe Taps',	'Dumbbell\r\nWorkout\r\nNUMBER 2\r\n\r\nEQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Advanced',	'- Lie on ground with legs straight up in air (body making a 90 degree angle).\r\n- Hold a single DB with both hands and use your abs to try and touch the DB\r\nto your toes and then back down.',	'2025-04-23 11:34:52',	'2025-04-23 11:34:52'),
(82,	NULL,	2,	'15 Bicep Curls',	'Bicep curls are an exercise that strengthens the biceps muscle in the upper arm by bending the elbow and raising a weight towards the shoulder. They can be performed with dumbbells or barbells, and variations include standing, seated, or incline curls.',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Intermediate',	'1. Start:\r\nStand with feet shoulder-width apart, hold a dumbbell in each hand with palms facing up, and keep your elbows close to your torso.\r\n2. Curl:\r\nBend at the elbows, bringing the dumbbells up to shoulder level, while keeping your upper arms stationary.\r\n3. Contract:\r\nSqueeze your biceps at the top of the curl and slowly lower the dumbbells back to the starting position.\r\n4. Repeat:\r\nContinue for the desired number of repetitions. ',	'2025-04-23 11:36:11',	'2025-04-23 11:36:11'),
(83,	NULL,	1,	'20 R Curtsy Lunge, Sumo Squat, L Curtsy Lunge**',	'EQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Intermediate',	'- Sumo squat = wide-stanced squat, toes pointed outwards.\r\n- Hold single DB in whatever position feels comfortable.',	'2025-04-23 11:38:11',	'2025-04-23 11:38:11'),
(84,	NULL,	1,	'20 R Curtsy Lunge, Sumo Squat, L Curtsy Lunge**',	'EQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	4,	'Intermediate',	'- Sumo squat = wide-stanced squat, toes pointed outwards.\r\n- Hold single DB in whatever position feels comfortable.',	'2025-04-23 11:38:35',	'2025-04-23 11:38:35'),
(85,	NULL,	2,	'15 Bicep Curl + Arnold Press',	'EQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	NULL,	'none',	NULL,	NULL,	3,	'Intermediate',	'- Arnold press** = Hold DB\'s in front of you with arms bent, hands facing you.\r\nraise arms above, turning hands 180 degrees so once they reach the top,\r\npalms are facing away from you. Return to starting position.',	'2025-04-23 11:40:49',	'2025-04-23 11:40:49'),
(86,	NULL,	2,	'15 Bicep Curl + Arnold Press',	'EQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Intermediate',	'- Arnold press** = Hold DB\'s in front of you with arms bent, hands facing you.\r\nraise arms above, turning hands 180 degrees so once they reach the top,\r\npalms are facing away from you. Return to starting position.',	'2025-04-23 11:41:02',	'2025-04-23 11:41:02'),
(87,	NULL,	2,	'15 Romanian Deadlift (RDL)**',	'EQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Intermediate',	'- Stand holding DB\'s in front of your thighs.\r\n- Keep your legs relatively straight and hinge back at the hips while you lower\r\nDB\'s down your legs till you feel a stretch. Slowly reverse the movement.',	'2025-04-23 11:42:11',	'2025-04-23 11:42:11'),
(88,	NULL,	5,	'20 DB slides + Optional Pushup',	'EQUIPMENT NEEDED:\r\n- 2 DUMBBELLS (DB)\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	2,	'Intermediate',	'- Get into a plank position (on hands). Place 1 DB on the floor by L hand.\r\n- Use the R hand to slide the object to the R side. Optional pushup. Repeat with L hand.',	'2025-04-23 11:43:20',	'2025-04-23 11:43:20'),
(89,	NULL,	5,	'Resistance Band Workout',	'Resistance\r\nBand Workout\r\n\r\nEQUIPMENT NEEDED:\r\n- 1 RESISTANCE BAND\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Intermediate',	'30 Banded Lunges',	'2025-04-23 11:45:47',	'2025-04-23 11:45:47'),
(90,	NULL,	5,	'30 Banded Single Romanian Deadlifts',	'Resistance\r\nBand Workout\r\n\r\nEQUIPMENT NEEDED:\r\n- 1 RESISTANCE BAND\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=yq0ehdbBZow',	'youtube',	NULL,	NULL,	3,	'Intermediate',	'30 Banded Single Romanian Deadlifts',	'2025-04-23 11:46:25',	'2025-04-23 11:46:25'),
(91,	NULL,	2,	'40 Banded Rows',	'Resistance\r\nBand Workout\r\n\r\nEQUIPMENT NEEDED:\r\n- 1 RESISTANCE BAND\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=-ei_xdLpUHo',	'youtube',	NULL,	NULL,	3,	'Intermediate',	'40 Banded Rows',	'2025-04-23 11:47:58',	'2025-04-23 11:47:58'),
(92,	NULL,	2,	'10 Banded Pushups',	'Resistance\r\nBand Workout\r\n\r\nEQUIPMENT NEEDED:\r\n- 1 RESISTANCE BAND\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=-ei_xdLpUHo',	'youtube',	NULL,	NULL,	3,	'Beginner',	'10 Banded Pushups',	'2025-04-23 11:49:29',	'2025-04-23 11:49:29'),
(93,	NULL,	2,	'25 Band Pull Aparts',	'Resistance\r\nBand Workout\r\n\r\nEQUIPMENT NEEDED:\r\n- 1 RESISTANCE BAND\r\n\r\nREST FOR 1-2 MINS AFTER\r\nCOMPLETING EXERCISES 1-5\r\nREPEAT X 3-5',	'https://www.youtube.com/watch?v=-ei_xdLpUHo',	'youtube',	NULL,	NULL,	3,	'Beginner',	'25 Band Pull Aparts',	'2025-04-23 11:50:14',	'2025-04-23 11:50:14');

DROP TABLE IF EXISTS `Messages`;
CREATE TABLE `Messages` (
  `message_id` int NOT NULL AUTO_INCREMENT,
  `sender_id` int NOT NULL,
  `sender_type` enum('therapist','patient','user') NOT NULL,
  `recipient_id` int NOT NULL,
  `recipient_type` enum('therapist','patient','user') NOT NULL,
  `subject` varchar(100) DEFAULT NULL,
  `content` text NOT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_sender` (`sender_id`,`sender_type`),
  KEY `idx_recipient` (`recipient_id`,`recipient_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_recipient_is_read` (`recipient_id`,`is_read`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


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
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`progress_id`),
  KEY `patient_id` (`patient_id`),
  KEY `plan_exercise_id` (`plan_exercise_id`),
  CONSTRAINT `PatientExerciseProgress_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `PatientExerciseProgress_ibfk_2` FOREIGN KEY (`plan_exercise_id`) REFERENCES `TreatmentPlanExercises` (`plan_exercise_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `PatientExerciseProgress` (`progress_id`, `patient_id`, `plan_exercise_id`, `completion_date`, `sets_completed`, `repetitions_completed`, `duration_seconds`, `pain_level`, `difficulty_level`, `notes`, `created_at`) VALUES
(40,	26,	38,	'2025-05-04',	3,	10,	300,	10,	10,	'ez',	'2025-05-03 16:58:03');

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
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`metric_id`),
  KEY `patient_id` (`patient_id`),
  KEY `therapist_id` (`therapist_id`),
  CONSTRAINT `PatientMetrics_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `PatientMetrics_ibfk_2` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `PatientNotes`;
CREATE TABLE `PatientNotes` (
  `note_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `therapist_id` int NOT NULL,
  `appointment_id` int DEFAULT NULL,
  `note_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`note_id`),
  KEY `patient_id` (`patient_id`),
  KEY `therapist_id` (`therapist_id`),
  KEY `appointment_id` (`appointment_id`),
  CONSTRAINT `PatientNotes_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `PatientNotes_ibfk_2` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE,
  CONSTRAINT `PatientNotes_ibfk_3` FOREIGN KEY (`appointment_id`) REFERENCES `Appointments` (`appointment_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `Patients`;
CREATE TABLE `Patients` (
  `patient_id` int NOT NULL AUTO_INCREMENT,
  `therapist_id` int NOT NULL,
  `first_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `last_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `diagnosis` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` enum('Active','Inactive','At Risk') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'Active',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` int DEFAULT NULL,
  PRIMARY KEY (`patient_id`),
  UNIQUE KEY `email` (`email`),
  KEY `therapist_id` (`therapist_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_patients_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `Patients_ibfk_1` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Patients` (`patient_id`, `therapist_id`, `first_name`, `last_name`, `email`, `phone`, `date_of_birth`, `address`, `diagnosis`, `status`, `notes`, `created_at`, `updated_at`, `user_id`) VALUES
(26,	17,	'111',	'',	'1@gmail.com',	NULL,	NULL,	NULL,	NULL,	'Active',	NULL,	'2025-05-03 16:51:24',	'2025-05-03 16:51:24',	57);

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
  `first_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `last_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `company_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `profile_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'avatar-2.jpg',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `experience_years` int DEFAULT '0',
  `specialties` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `education` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `languages` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
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
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `Therapists` (`id`, `first_name`, `last_name`, `company_email`, `password`, `profile_image`, `bio`, `experience_years`, `specialties`, `education`, `languages`, `address`, `rating`, `review_count`, `is_accepting_new_patients`, `average_session_length`) VALUES
(17,	'Wards',	'Sa red',	'1@gmail.com',	'$2b$12$jtCoUYTUdpyUFu.JHz/qJ.5vOqZR/FO1L4tHbV/2ijxcjQyIMQpUi',	'therapist_17_1746029607.png',	'',	0,	'[]',	'[]',	'[]',	'',	0,	0,	1,	60);

DROP TABLE IF EXISTS `TreatmentPlanExercises`;
CREATE TABLE `TreatmentPlanExercises` (
  `plan_exercise_id` int NOT NULL AUTO_INCREMENT,
  `plan_id` int NOT NULL,
  `exercise_id` int NOT NULL,
  `sets` int DEFAULT '1',
  `repetitions` int DEFAULT NULL,
  `frequency` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `duration` int DEFAULT NULL,
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`plan_exercise_id`),
  KEY `plan_id` (`plan_id`),
  KEY `exercise_id` (`exercise_id`),
  CONSTRAINT `TreatmentPlanExercises_ibfk_1` FOREIGN KEY (`plan_id`) REFERENCES `TreatmentPlans` (`plan_id`) ON DELETE CASCADE,
  CONSTRAINT `TreatmentPlanExercises_ibfk_2` FOREIGN KEY (`exercise_id`) REFERENCES `Exercises` (`exercise_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `TreatmentPlanExercises` (`plan_exercise_id`, `plan_id`, `exercise_id`, `sets`, `repetitions`, `frequency`, `duration`, `notes`, `created_at`) VALUES
(38,	18,	16,	3,	10,	'Every other day',	10,	'',	'2025-05-03 16:52:52');

DROP TABLE IF EXISTS `TreatmentPlans`;
CREATE TABLE `TreatmentPlans` (
  `plan_id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `therapist_id` int NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `status` enum('Active','Completed','Cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'Active',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`plan_id`),
  KEY `patient_id` (`patient_id`),
  KEY `therapist_id` (`therapist_id`),
  CONSTRAINT `TreatmentPlans_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `Patients` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `TreatmentPlans_ibfk_2` FOREIGN KEY (`therapist_id`) REFERENCES `Therapists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `TreatmentPlans` (`plan_id`, `patient_id`, `therapist_id`, `name`, `description`, `start_date`, `end_date`, `status`, `created_at`, `updated_at`) VALUES
(18,	26,	17,	'Biking improvement',	'',	'2025-05-03',	'2025-06-28',	'Active',	'2025-05-03 16:52:52',	'2025-05-03 16:52:52');

DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `feedback_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
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
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `profile_pic` varchar(255) COLLATE utf8mb4_general_ci DEFAULT 'avatar-2.jpg',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `users` (`user_id`, `username`, `email`, `password_hash`, `profile_pic`, `created_at`, `updated_at`) VALUES
(57,	'111',	'1@gmail.com',	'$2b$12$2qMMMZ3LfhCkDs9fO0v2FOjSob1H.wHk/vDVrERiyiprQEzl7aaq.',	'avatar-2.jpg',	'2025-04-30 17:02:55',	'2025-04-30 17:02:55'),
(59,	'cat',	'cat@gmail.com',	'$2b$12$uAtgLYY1fYi6CLHzWYirjujwOAKzIzM3U4OkduKxDUIIM1RXxZJPi',	'avatar-2.jpg',	'2025-05-01 13:31:27',	'2025-05-01 13:31:27'),
(62,	'212',	'a@gmail.com',	'$2b$12$T69LJAehPH40SPv/rNQnC.7A5MTcJLiEC167by7.NkDbxykYRjIQq',	'avatar-2.jpg',	'2025-05-03 11:32:16',	'2025-05-03 11:32:16');

-- 2025-05-07 06:27:40 UTC
