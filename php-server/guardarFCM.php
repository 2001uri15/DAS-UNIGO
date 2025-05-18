<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// Parámetros de conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

// Respuesta por defecto
$response = array('success' => false, 'message' => '');

try {
    // Verificar que la solicitud sea POST
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        throw new Exception('Método no permitido. Se requiere POST');
    }

    // Obtener el token del $_POST
    if (!isset($_POST['tokenFCM']) || empty($_POST['tokenFCM'])) {
        throw new Exception('Token FCM no proporcionado');
    }
    
    $tokenFCM = $_POST['tokenFCM'];
    
    // Validar que el token tenga un formato razonable
    if (strlen($tokenFCM) > 256) {
        throw new Exception('El token FCM excede la longitud máxima permitida');
    }
    
    // Conectar a la base de datos
    $conn = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Verificar si el token ya existe
    $stmt = $conn->prepare("SELECT COUNT(*) FROM `app_tokenFCM` WHERE `tokenFCM` = :token");
    $stmt->bindParam(':token', $tokenFCM);
    $stmt->execute();
    
    $tokenExists = $stmt->fetchColumn();
    
    if ($tokenExists) {
        $response['message'] = 'El token FCM ya existe en la base de datos';
        $response['success'] = true;
    } else {
        // Insertar el nuevo token
        $stmt = $conn->prepare("INSERT INTO `app_tokenFCM` (`tokenFCM`) VALUES (:token)");
        $stmt->bindParam(':token', $tokenFCM);
        $stmt->execute();
        
        $response['success'] = true;
        $response['message'] = 'Token FCM guardado correctamente';
    }
    
} catch (PDOException $e) {
    $response['message'] = 'Error de base de datos: ' . $e->getMessage();
} catch (Exception $e) {
    $response['message'] = 'Error: ' . $e->getMessage();
}

echo json_encode($response);
?>
