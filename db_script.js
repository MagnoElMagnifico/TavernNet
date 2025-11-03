const db = connect("mongodb://localhost:27017/tavernnet")

const zarion = new ObjectId();
const jolly = new ObjectId();

db.users.insertMany([
    { _id: "marcos",   password: "1234", active: null,   creation: new Date() },
    { _id: "jeremias", password: "abcd", active: zarion, creation: new Date() }
])

db.characters.insertMany([
    {
        _id: zarion,
        name: "Zarion",
        user: "jeremias",
        creation: new Date(),

        biography: "Lorem ipsum dolor sit amet.",
        alignment: "lawful good",
        race: "human",
        languages: ["Common", "Draconic", "Elfic"],
        stats: {
            main: {
                CON: 10,
                STR: 10
            },
            combat: {
                HP: 8,
                AC: 12,
                speed: 30,
                initiative: 4,
            }
        }
    },
    {
        _id: jolly,
        name: "Jolly Jostar",
        user: "jeremias",
        creation: new Date(),

        biography: "Lorem ipsum dolor sit amet.",
        alignment: "lawful good",
        race: "tifling",
        languages: ["Common", "Draconic", "Elfic"],
        stats: {
            main: {
                CON: 10,
                STR: 10
            },
            combat: {
                HP: 8,
                AC: 12,
                speed: 30,
                initiative: 4,
            }
        }
    }
])

const post1 = new ObjectId();
const post2 = new ObjectId();

db.posts.insertMany([
    {
        _id: post1,
        date: new Date("2025-10-08T08:10:30.123Z"),
        character: zarion,
        title: "Post de prueba",
        content: "lorem ipsum dolor sit amet",
    },
    {
        _id: post2,
        date: new Date("2025-10-08T18:18:19.233Z"),
        character: jolly,
        title: "pues otro post de prueba",
        content: "otro texto de ejemplo",
    }
])

db.comments.insertMany([
    {
        _id: {
            post: post1,
            author: zarion,
            date: new Date("2025-10-02T06:01:18.234Z"),
        },
        content: "primer comentario?"
    },
    {
        _id: {
            post: post1,
            author: zarion,
            date: new Date(),
        },
        content: "buen post"
    }
])

db.likes.insertMany([
    {
        _id: {
            post: post1,
            author: jolly,
        }
    }
])

// Acceso eficiente a personajes por nombre de usuario y nombre de personaje
// Tampoco permite nombres de usuario y personajes repetidos
db.characters.createIndex({ user: 1, name: 1 }, { unique: true })

// TODO: esta vista no acaba de funcionar bien
// Crear una vista para comprobar fácilmente el número de comentarios y likes
//
// NOTAS SOBRE RENDIMIENTO
// Crea una vista que se traducirá en consultas (no es materializada), por lo
// que no es lo mejor cuando hay muchas consultas y documentos. Pero como esto
// no es el objetivo de la práctica, así está lo suficientemente bien.
//
// Por otro lado, estas vistas son solo de lectura: no se pueden escribir en
// ellas, sino que se debe hacer en las colecciones subyacentes.
db.createView(
    "posts_view", // nombre de la vista a crear
    "posts",      // colección de origen
    [
        // Hacer lookup de likes
        {
            $lookup: {
                from: "likes",
                localField: "_id",
                foreignField: "_id.post",
                as: "likes_docs"
            }
        },

        // Hacer lookup de comentarios
        {
            $lookup: {
                from: "comments",
                localField: "_id",
                foreignField: "_id.post",
                as: "comments_docs"
            }
        },

        // Calcular totales
        {
            $addFields: {
                n_likes: { $size: "$likes_docs" },
                n_comments: { $size: "$comments_docs" }
            }
        },

        // Ocultar los arrays completos
        {
            $project: {
                likes_docs: 0,
                comments_docs: 0
            }
        }
    ]
);
