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

// Recibir datos POST con nombres alternativos
$nombre     = $_POST['nombre']     ?? '';
$apellido   = $_POST['apellido']   ?? '';
$mail       = $_POST['mail']       ?? '';
$contrasena = $_POST['password']   ?? ''; // Acepta ambos nombres

// Validaciones básicas
if (empty($nombre) || empty($apellido) || empty($mail) || empty($contrasena)) {
    echo json_encode([
        "status" => "error", 
        "message" => "Todos los campos obligatorios deben ser enviados",
        "received_data" => $_POST // Para depuración
    ]);
    exit();
}

// Comprobar si ya existe el usuario
$stmt_check = $conn->prepare("SELECT id FROM app_user WHERE mail = ?");
$stmt_check->bind_param("s", $mail);
$stmt_check->execute();
$stmt_check->store_result();

if ($stmt_check->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "El usuario o correo ya existe"]);
    exit();
}
$stmt_check->close();

// Insertar nuevo usuario (sin foto)
$stmt_insert = $conn->prepare("INSERT INTO Xalarrazabal025_usuarios (nombre, apellido, mail, contra) VALUES (?, ?, ?, ?)");
$hashedPassword = md5($contrasena);  // Para más seguridad: usar password_hash()
$stmt_insert->bind_param("ssss", $nombre, $apellido, $mail, $hashedPassword);

if ($stmt_insert->execute()) {
    $idUsuario = $stmt_insert->insert_id;

    // Crear token y guardar sesión
    $token = bin2hex(random_bytes(32));
    $fecha = date('Y-m-d H:i:s');

    $stmt_token = $conn->prepare("INSERT INTO app_token (idUser, token) VALUES (?, ?)");
    $stmt_token->bind_param("is", $idUsuario, $token);
    $stmt_token->execute();
    $stmt_token->close();

    echo json_encode([
        "status"    => "success",
        "message"   => "Registro exitoso",
        "token"     => $token,
        "nombre"    => $nombre,
        "apellido"  => $apellido,
        "mail"      => $mail
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Error al registrar usuario"]);
}

$stmt_insert->close();
$conn->close();
?>
