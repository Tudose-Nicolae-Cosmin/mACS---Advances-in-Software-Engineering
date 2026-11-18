import argparse
import random
import sqlite3
import time
import uuid
from datetime import datetime, timedelta, timezone

import bcrypt
from faker import Faker

parser = argparse.ArgumentParser(description="Populeaza instakill.db cu date de test")
parser.add_argument("--db", default="backend/instakill.db", help="calea catre fisierul SQLite")
parser.add_argument("--users", type=int, default=10000, help="numarul TOTAL de useri (inclusiv alice, bob, charlie)")
parser.add_argument("--min-posts", type=int, default=10, help="postari minime per user")
parser.add_argument("--max-posts", type=int, default=20, help="postari maxime per user")
parser.add_argument("--max-comments", type=int, default=10, help="comentarii maxime per postare")
parser.add_argument("--max-likes", type=int, default=20, help="like-uri maxime per postare")
parser.add_argument("--batch", type=int, default=500, help="cati useri se proceseaza inainte de fiecare commit")
args = parser.parse_args()

fake = Faker()
now = datetime.now(timezone.utc)
started = time.time()

conn = sqlite3.connect(args.db)

tables = {row[0] for row in conn.execute("SELECT name FROM sqlite_master WHERE type = 'table'")}
if not {"users", "posts", "likes", "comments"} <= tables:
    raise SystemExit("Nu gasesc tabelele users/posts/likes/comments. Porneste aplicatia o data ca sa le creeze.")

row = conn.execute("SELECT typeof(created_at) FROM users WHERE username IN ('alice', 'bob') LIMIT 1").fetchone()
SAVE_AS_MILLIS = row is not None and row[0] in ("integer", "real")


def to_db(dt):
    """Transforma un datetime (UTC) in valoarea care se scrie in baza de date."""
    if SAVE_AS_MILLIS:
        return int(dt.timestamp() * 1000)
    return dt.strftime("%Y-%m-%dT%H:%M:%S.") + f"{dt.microsecond // 1000:03d}Z"


conn.execute("DELETE FROM likes")
conn.execute("DELETE FROM comments")
conn.execute("DELETE FROM posts")
conn.execute("DELETE FROM users WHERE username NOT IN ('alice', 'bob')")
conn.commit()
print("Date vechi sterse (alice si bob pastrati).")

password_hash = bcrypt.hashpw(b"password", bcrypt.gensalt(10)).decode()

user_ids = [r[0] for r in conn.execute("SELECT id FROM users")]

charlie_id = str(uuid.uuid4())
conn.execute(
    "INSERT INTO users (id, username, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)",
    (charlie_id, "charlie", "charlie@example.com", password_hash, to_db(now)))
user_ids.append(charlie_id)

new_users = []
for i in range(max(0, args.users - len(user_ids))):
    username = f"{fake.user_name()}{i}"
    created = fake.date_time_between(start_date="-1y", end_date="now", tzinfo=timezone.utc)
    new_id = str(uuid.uuid4())
    new_users.append((new_id, username, f"{username}@example.com", password_hash, to_db(created)))
    user_ids.append(new_id)

conn.executemany(
    "INSERT INTO users (id, username, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)", new_users)
conn.commit()
print(f"Useri in baza: {len(user_ids)} (inclusiv alice, bob, charlie)")

week_ago = now - timedelta(days=7)
sentences = [fake.sentence(nb_words=6) for _ in range(300)]

total_posts = total_likes = total_comments = 0

for start in range(0, len(user_ids), args.batch):
    post_rows, like_rows, comment_rows = [], [], []

    for author_id in user_ids[start:start + args.batch]:
        for _ in range(random.randint(args.min_posts, args.max_posts)):
            post_id = str(uuid.uuid4())

            day = fake.date_between_dates(date_start=week_ago.date(), date_end=now.date())
            created = datetime(day.year, day.month, day.day, tzinfo=timezone.utc) + timedelta(seconds=random.randint(0, 86399))
            if created > now:
                created = now - timedelta(seconds=random.randint(0, 3600))
            seconds_since_post = int((now - created).total_seconds())

            likers = random.sample(user_ids, min(random.randint(0, args.max_likes), len(user_ids)))
            for liker in likers:
                liked_at = created + timedelta(seconds=random.randint(0, seconds_since_post))
                like_rows.append((str(uuid.uuid4()), post_id, liker, to_db(liked_at)))

            n_comments = random.randint(0, args.max_comments)
            for _ in range(n_comments):
                commented_at = created + timedelta(seconds=random.randint(0, seconds_since_post))
                comment_rows.append((str(uuid.uuid4()), post_id, random.choice(user_ids),
                                     random.choice(sentences), to_db(commented_at)))

            image_url = f"/media/{uuid.uuid4()}." + random.choice(["jpg", "mp4"])
            content = " ".join(fake.words(nb=random.randint(1, 10)))

            post_rows.append((post_id, author_id, content, image_url, to_db(created), to_db(created),
                              len(likers), n_comments))

    conn.executemany(
        "INSERT INTO posts (id, author_id, content, image_url, created_at, updated_at, like_count, comment_count) "
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", post_rows)
    conn.executemany(
        "INSERT INTO likes (id, post_id, user_id, created_at) VALUES (?, ?, ?, ?)", like_rows)
    conn.executemany(
        "INSERT INTO comments (id, post_id, author_id, content, created_at) VALUES (?, ?, ?, ?, ?)", comment_rows)
    conn.commit()

    total_posts += len(post_rows)
    total_likes += len(like_rows)
    total_comments += len(comment_rows)
    print(f"  {min(start + args.batch, len(user_ids))}/{len(user_ids)} useri procesati", end="\r")

print()

print("Creez indexurile (daca lipsesc)...")
conn.execute("CREATE INDEX IF NOT EXISTS idx_posts_author ON posts (author_id)")
conn.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_likes_post_user ON likes (post_id, user_id)")
conn.execute("CREATE INDEX IF NOT EXISTS idx_comments_post ON comments (post_id)")
conn.commit()

checks = {
    "postari cu autor inexistent": "SELECT COUNT(*) FROM posts WHERE author_id NOT IN (SELECT id FROM users)",
    "like-uri fara postare": "SELECT COUNT(*) FROM likes WHERE post_id NOT IN (SELECT id FROM posts)",
    "like-uri fara user": "SELECT COUNT(*) FROM likes WHERE user_id NOT IN (SELECT id FROM users)",
    "comentarii fara postare": "SELECT COUNT(*) FROM comments WHERE post_id NOT IN (SELECT id FROM posts)",
    "comentarii fara autor": "SELECT COUNT(*) FROM comments WHERE author_id NOT IN (SELECT id FROM users)",
    "postari cu like_count gresit": "SELECT COUNT(*) FROM posts p LEFT JOIN (SELECT post_id, COUNT(*) n FROM likes GROUP BY post_id) x ON x.post_id = p.id WHERE p.like_count != COALESCE(x.n, 0)",
    "postari cu comment_count gresit": "SELECT COUNT(*) FROM posts p LEFT JOIN (SELECT post_id, COUNT(*) n FROM comments GROUP BY post_id) x ON x.post_id = p.id WHERE p.comment_count != COALESCE(x.n, 0)",
}
print("Verificare integritate (toate trebuie sa fie 0):")
for name, sql in checks.items():
    print(f"  {name}: {conn.execute(sql).fetchone()[0]}")

conn.close()
print(f"Gata: {len(user_ids)} useri, {total_posts} postari, {total_likes} like-uri, "
      f"{total_comments} comentarii, in {time.time() - started:.1f} secunde.")