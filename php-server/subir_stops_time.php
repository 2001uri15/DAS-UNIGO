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
    
    // Preparar consulta SQL
    $sql = "INSERT INTO bus_stop_times (
        trip_id, arrival_time, departure_time, stop_id, stop_sequence, 
        stop_headsign, pickup_type, drop_off_type, timepoint, 
        continuous_pickup, continuous_drop_off
    ) VALUES (
        :trip_id, :arrival_time, :departure_time, :stop_id, :stop_sequence, 
        :stop_headsign, :pickup_type, :drop_off_type, :timepoint, 
        :continuous_pickup, :continuous_drop_off
    ) ON DUPLICATE KEY UPDATE
        arrival_time = VALUES(arrival_time),
        departure_time = VALUES(departure_time),
        stop_id = VALUES(stop_id),
        stop_headsign = VALUES(stop_headsign),
        pickup_type = VALUES(pickup_type),
        drop_off_type = VALUES(drop_off_type),
        timepoint = VALUES(timepoint),
        continuous_pickup = VALUES(continuous_pickup),
        continuous_drop_off = VALUES(continuous_drop_off)";
    
    $stmt = $conn->prepare($sql);
    
    // Bind parameters
    $stmt->bindParam(':trip_id', $data->trip_id);
    $stmt->bindParam(':arrival_time', $data->arrival_time);
    $stmt->bindParam(':departure_time', $data->departure_time);
    $stmt->bindParam(':stop_id', $data->stop_id);
    $stmt->bindParam(':stop_sequence', $data->stop_sequence);
    $stmt->bindParam(':stop_headsign', $data->stop_headsign);
    $stmt->bindParam(':pickup_type', $data->pickup_type, PDO::PARAM_INT);
    $stmt->bindParam(':drop_off_type', $data->drop_off_type, PDO::PARAM_INT);
    $stmt->bindParam(':timepoint', $data->timepoint, PDO::PARAM_INT);
    $stmt->bindParam(':continuous_pickup', $data->continuous_pickup, PDO::PARAM_INT);
    $stmt->bindParam(':continuous_drop_off', $data->continuous_drop_off, PDO::PARAM_INT);
    
    // Ejecutar consulta
    if($stmt->execute()) {
        http_response_code(200);
        echo json_encode(array("message" => "Stop time actualizado correctamente."));
    } else {
        http_response_code(503);
        echo json_encode(array("message" => "No se pudo actualizar el stop time."));
    }
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(array("message" => "Error de base de datos: " . $e->getMessage()));
}
?>
