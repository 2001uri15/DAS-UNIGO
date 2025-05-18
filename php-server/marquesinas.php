<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Parámetros de conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

try {
    // Crear conexión PDO
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Consulta SQL para obtener todas las paradas
    $sql = "SELECT `stop_id`, `stop_code`, `stop_name`, `stop_lat`, `stop_lon`, `zone_id`, `stop_timezone` FROM `bus_stop` WHERE 1";
    $stmt = $conn->prepare($sql);
    $stmt->execute();
    
    // Obtener resultados como array asociativo
    $stops = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Devolver resultados como JSON
    echo json_encode($stops);
    
} catch(PDOException $e) {
    // En caso de error, devolver mensaje de error
    echo json_encode(array("error" => "Connection failed: " . $e->getMessage()));
}

$conn = null;
?>
