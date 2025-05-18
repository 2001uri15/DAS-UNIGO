<?php
// Conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $password);
} catch (PDOException $e) {
    die("Error de conexión: " . $e->getMessage());
}

// Consulta los datos de rutas
$sql = "SELECT shape_id, shape_pt_lat, shape_pt_lon, shape_pt_sequence 
        FROM bus_shape 
        ORDER BY shape_id, shape_pt_sequence";

$stmt = $pdo->prepare($sql);
$stmt->execute();

$routes = [];
while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
    $routes[$row['shape_id']][] = [
        'lat' => (float)$row['shape_pt_lat'],
        'lon' => (float)$row['shape_pt_lon']
    ];
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Mapa de Rutas</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <style>
        #map { height: 100vh; width: 100%; }
    </style>
</head>
<body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        const map = L.map('map').setView([42.8485668, -2.6767569], 12); // Puedes ajustar la vista inicial

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(map);

        // Colores predefinidos (si hay más rutas, se repiten)
        const colors = ['red', 'blue', 'green', 'orange', 'purple', 'brown', 'black', 'cyan', 'magenta', 'darkgreen'];

        const rutas = <?php echo json_encode($routes); ?>;
        let colorIndex = 0;

        for (const [shapeId, puntos] of Object.entries(rutas)) {
            const latlngs = puntos.map(p => [p.lat, p.lon]);
            const color = colors[colorIndex % colors.length];
            colorIndex++;

            L.polyline(latlngs, { color: color, weight: 4 }).addTo(map)
                .bindPopup("Ruta: " + shapeId);
        }
    </script>
</body>
</html>

