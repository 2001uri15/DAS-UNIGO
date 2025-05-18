<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Parámetros de conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

// Obtener datos del POST
$data = json_decode(file_get_contents("php://input"));

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Consulta SQL
    $sql = "INSERT INTO bus_shape (
        shape_id, shape_pt_lat, shape_pt_lon, shape_pt_sequence, shape_dist_traveled
    ) VALUES (
        :shape_id, :shape_pt_lat, :shape_pt_lon, :shape_pt_sequence, :shape_dist_traveled
    ) ON DUPLICATE KEY UPDATE
        shape_pt_lat = VALUES(shape_pt_lat),
        shape_pt_lon = VALUES(shape_pt_lon),
        shape_dist_traveled = VALUES(shape_dist_traveled)";
    
    $stmt = $conn->prepare($sql);
    
    // Bind de parámetros
    $stmt->bindParam(':shape_id', $data->shape_id);
    $stmt->bindParam(':shape_pt_lat', $data->shape_pt_lat);
    $stmt->bindParam(':shape_pt_lon', $data->shape_pt_lon);
    $stmt->bindParam(':shape_pt_sequence', $data->shape_pt_sequence);
    $stmt->bindParam(':shape_dist_traveled', $data->shape_dist_traveled);
    
    // Ejecutar consulta
    if ($stmt->execute()) {
        http_response_code(200);
        echo json_encode(array("message" => "Shape insertado o actualizado correctamente."));
    } else {
        http_response_code(503);
        echo json_encode(array("message" => "No se pudo insertar el shape."));
    }
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(array("message" => "Error de base de datos: " . $e->getMessage()));
}
?>

