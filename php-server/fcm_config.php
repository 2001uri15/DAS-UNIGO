<?php
// Configuración básica
define('FCM_SERVICE_ACCOUNT_PATH', __DIR__.'/firebase-service-account.json');
define('FCM_PROJECT_ID', 'unigo-2025');
define('FCM_API_URL', 'https://fcm.googleapis.com/v1/projects/'.FCM_PROJECT_ID.'/messages:send');
