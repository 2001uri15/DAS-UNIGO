<?php
header('Content-Type: application/json');

// Configuración de la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

try {
    // Conexión a la base de datos
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Consulta SQL para obtener las rutas
    $sql = "SELECT id, lat, lon FROM bici_ruta ORDER BY id, lon, lat";
    $stmt = $conn->prepare($sql);
    $stmt->execute();
    
    // Obtener los resultados como array asociativo
    $rutas = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Convertir a JSON y mostrarlo
    echo json_encode($rutas);
    
} catch(PDOException $e) {
    // En caso de error, devolver un JSON con el mensaje de error
    echo json_encode(array('error' => 'Error de conexión: ' . $e->getMessage()));
}
?>
