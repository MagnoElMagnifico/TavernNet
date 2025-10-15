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
        likes: 1
    },
    {
        _id: post2,
        date: new Date("2025-10-08T18:18:19.233Z"),
        character: jolly,
        title: "pues otro post de prueba",
        content: "otro texto de ejemplo",
        likes: 0
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

// Al crear/actualizar un nuevo personaje, se configura como activo para el usuario
// db.characters
//     .watch([
//         { $match: { operationType: { $in: ["insert", "update"] }}},
//         { fullDocument: "updateLookup" }
//     ])
//     .on("change", (change) => {
//         const newChar = change.fullDocument;
//         if (!newChar) return;
//         print(`Nuevo personaje creado: ${newChar.name} (${newChar._id})`);
//
//         // Asigna el personaje activo al usuario
//         db.users.updateOne(
//             { _id: newChar.user },
//             { $set: { active: newChar._id } }
//         );
//     });

