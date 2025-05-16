<?php
header('Content-Type: application/json; charset=utf-8');

// Conexión a la base de datos
$servername = "localhost";
$username = "Xalarrazabal025"; 
$password = "vRN7UMCCFV"; 
$dbname = "Xalarrazabal025_unigo"; 


$conn = new mysqli($servername, $username, $password, $dbname);

// Verificar conexión
if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Error de conexión"]));
}

$conn->set_charset("utf8mb4");

// Recoger token desde POST o GET
$token = $_POST['token'] ?? $_GET['token'] ?? '';

if (empty($token)) {
    echo json_encode(["status" => "error", "message" => "Token no proporcionado"]);
    exit();
}

// Eliminar el token
$stmt = $conn->prepare("DELETE FROM app_token WHERE token = ?");
$stmt->bind_param("s", $token);

if ($stmt->execute()) {
    if ($stmt->affected_rows > 0) {
        echo json_encode(["status" => "success", "message" => "Sesión cerrada correctamente"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Token no encontrado"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Error al cerrar sesión"]);
}

$stmt->close();
$conn->close();
?>

