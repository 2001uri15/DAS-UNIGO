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
    $sql = "INSERT INTO bus_trips (
        route_id, service_id, trip_id, trip_headsign, trip_short_name, 
        direction_id, block_id, shape_id, wheelchair_accessible, bikes_allowed
    ) VALUES (
        :route_id, :service_id, :trip_id, :trip_headsign, :trip_short_name,
        :direction_id, :block_id, :shape_id, :wheelchair_accessible, :bikes_allowed
    ) ON DUPLICATE KEY UPDATE
        trip_headsign = VALUES(trip_headsign),
        trip_short_name = VALUES(trip_short_name),
        direction_id = VALUES(direction_id),
        block_id = VALUES(block_id),
        shape_id = VALUES(shape_id),
        wheelchair_accessible = VALUES(wheelchair_accessible),
        bikes_allowed = VALUES(bikes_allowed)";

    $stmt = $conn->prepare($sql);

    // Bind de parámetros
    $stmt->bindParam(':route_id', $data->route_id);
    $stmt->bindParam(':service_id', $data->service_id);
    $stmt->bindParam(':trip_id', $data->trip_id);
    $stmt->bindParam(':trip_headsign', $data->trip_headsign);
    $stmt->bindParam(':trip_short_name', $data->trip_short_name);
    $stmt->bindParam(':direction_id', $data->direction_id);
    $stmt->bindParam(':block_id', $data->block_id);
    $stmt->bindParam(':shape_id', $data->shape_id);
    $stmt->bindParam(':wheelchair_accessible', $data->wheelchair_accessible);
    $stmt->bindParam(':bikes_allowed', $data->bikes_allowed);

    // Ejecutar consulta
    if ($stmt->execute()) {
        http_response_code(200);
        echo json_encode(array("message" => "Viaje insertado o actualizado correctamente."));
    } else {
        http_response_code(503);
        echo json_encode(array("message" => "No se pudo insertar el viaje."));
    }
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(array("message" => "Error de base de datos: " . $e->getMessage()));
}
?>
