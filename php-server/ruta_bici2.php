<?php
// Conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

header('Content-Type: text/html; charset=utf-8');

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Consulta para obtener todos los caminos
    $query = "SELECT id, direccion, tipologia, calle, longitud_metros, ST_AsText(geometria) as geometria FROM bici_camino";
    $stmt = $conn->prepare($query);
    $stmt->execute();
    $caminos = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
} catch(PDOException $e) {
    die("Error de conexión a la base de datos: " . $e->getMessage());
}
?>

<!DOCTYPE html>
<html>
<head>
    <title>Mapa de Rutas de Bicicleta</title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <!-- Leaflet CSS -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
    
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: Arial, sans-serif;
        }
        #map {
            width: 100%;
            height: 100vh;
        }
        .route-controls {
            position: absolute;
            top: 10px;
            right: 10px;
            z-index: 1000;
            background: white;
            padding: 10px;
            border-radius: 5px;
            box-shadow: 0 0 10px rgba(0,0,0,0.2);
        }
        .error {
            color: red;
            padding: 20px;
            background: #ffeeee;
            border: 1px solid #ffcccc;
        }
    </style>
</head>
<body>
    <?php if (empty($caminos)): ?>
        <div class="error">
            No se encontraron rutas de bicicleta en la base de datos o hubo un error al recuperarlas.
        </div>
    <?php endif; ?>
    
    <div id="map"></div>
    <div class="route-controls">
        <select id="route-selector">
            <option value="">Selecciona una ruta</option>
            <?php foreach ($caminos as $camino): ?>
                <?php if (!empty($camino['geometria'])): ?>
                    <option value="<?php echo htmlspecialchars($camino['id']); ?>">
                        <?php echo "Ruta " . htmlspecialchars($camino['id']) . " - " . htmlspecialchars($camino['calle']); ?>
                    </option>
                <?php endif; ?>
            <?php endforeach; ?>
        </select>
    </div>

    <!-- Leaflet JS -->
    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
    
    <script>
        // Inicializar el mapa
        var map = L.map('map').setView([43.318334, -1.981231], 13);
        
        // Añadir capa de OpenStreetMap
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
        
        // Objeto para almacenar las capas de las rutas
        var routeLayers = {};
        
        <?php foreach ($caminos as $camino): ?>
            <?php if (!empty($camino['geometria']) && preg_match('/LINESTRING\((.+)\)/', $camino['geometria'], $matches)): ?>
                <?php 
                    $coords = explode(',', $matches[1]);
                    $points = array();
                    foreach ($coords as $coord) {
                        $coord = trim($coord);
                        if (preg_match('/^([\d\.]+) ([\d\.]+)$/', $coord, $coord_matches)) {
                            $points[] = array((float)$coord_matches[2], (float)$coord_matches[1]);
                        }
                    }
                ?>
                
                // Solo crear la capa si hay puntos válidos
                <?php if (!empty($points)): ?>
                    var polyline_<?php echo $camino['id']; ?> = L.polyline(
                        <?php echo json_encode($points); ?>,
                        {
                            color: '#' + Math.floor(Math.random()*16777215).toString(16),
                            weight: 5,
                            opacity: 0.7
                        }
                    );
                    
                    polyline_<?php echo $camino['id']; ?>.bindPopup(
                        '<b>Ruta <?php echo addslashes($camino['id']); ?></b><br>' +
                        'Calle: <?php echo addslashes($camino['calle']); ?><br>' +
                        'Tipología: <?php echo addslashes($camino['tipologia']); ?><br>' +
                        'Longitud: <?php echo addslashes($camino['longitud_metros']); ?> metros'
                    );
                    
                    routeLayers[<?php echo $camino['id']; ?>] = polyline_<?php echo $camino['id']; ?>;
                <?php endif; ?>
            <?php endif; ?>
        <?php endforeach; ?>
        
        // Controlador para el selector de rutas
        document.getElementById('route-selector').addEventListener('change', function(e) {
            // Eliminar todas las rutas del mapa
            for (var id in routeLayers) {
                if (map.hasLayer(routeLayers[id])) {
                    map.removeLayer(routeLayers[id]);
                }
            }
            
            // Añadir la ruta seleccionada
            var selectedId = e.target.value;
            if (selectedId && routeLayers[selectedId]) {
                map.addLayer(routeLayers[selectedId]);
                map.fitBounds(routeLayers[selectedId].getBounds());
            }
        });
    </script>
</body>
</html>
