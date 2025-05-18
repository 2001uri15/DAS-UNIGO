<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Configuración de la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

// Obtener datos del POST
$data = json_decode(file_get_contents("php://input"));

// Validar datos requeridos
if (!isset($data->geometria)) {
    http_response_code(400);
    echo json_encode(array("message" => "Falta el campo geometria (LINESTRING WKT)"));
    exit;
}

try {
    // Conexión a la base de datos
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Preparar consulta SQL
    $sql = "INSERT INTO bici_camino (
        direccion, 
        tipologia, 
        calle, 
        longitud_metros, 
        geometria
    ) VALUES (
        :direccion, 
        :tipologia, 
        :calle, 
        :longitud_metros, 
        ST_GeomFromText(:geometria)
    )";

    $stmt = $conn->prepare($sql);

    // Bind parameters
    $stmt->bindParam(':direccion', $data->direccion);
    $stmt->bindParam(':tipologia', $data->tipologia);
    $stmt->bindParam(':calle', $data->calle);
    $stmt->bindParam(':longitud_metros', $data->longitud_metros);
    $stmt->bindParam(':geometria', $data->geometria);

    if ($stmt->execute()) {
        $lastId = $conn->lastInsertId();
        http_response_code(201);
        echo json_encode(array(
            "message" => "Camino creado exitosamente",
            "id" => $lastId
        ));
    } else {
        http_response_code(503);
        echo json_encode(array("message" => "Error al crear el camino"));
    }

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(array(
        "message" => "Error de base de datos: " . $e->getMessage(),
        "error_details" => $e->errorInfo
    ));
}
?>
