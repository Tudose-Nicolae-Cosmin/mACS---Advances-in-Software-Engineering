import argparse
import random
import time
import uuid
from datetime import datetime, timedelta, timezone

import bcrypt
import psycopg2
from faker import Faker
from psycopg2.extras import execute_values

parser = argparse.ArgumentParser(description="Populeaza baza PostgreSQL Instakill cu date de test")
parser.add_argument("--host", default="localhost")
parser.add_argument("--port", type=int, default=5432)
parser.add_argument("--dbname", default="instakill")
parser.add_argument("--user", default="instakill")
parser.add_argument("--password", default="instakill_password")
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

conn = psycopg2.connect(host=args.host, port=args.port, dbname=args.dbname, user=args.user, password=args.password)
cur = conn.cursor()

cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
tables = {row[0] for row in cur.fetchall()}
if not {"users", "posts", "likes", "comments"} <= tables:
    raise SystemExit("Nu gasesc tabelele users/posts/likes/comments. Porneste aplicatia o data cu profilul postgres.")

cur.execute("TRUNCATE likes, comments, posts")
cur.execute("DELETE FROM users WHERE username NOT IN ('alice', 'bob')")
conn.commit()
print("Date vechi sterse (alice si bob pastrati).")

password_hash = bcrypt.hashpw(b"password", bcrypt.gensalt(10)).decode()

cur.execute("SELECT id FROM users")
user_ids = [row[0].strip() for row in cur.fetchall()]

charlie_id = str(uuid.uuid4())
user_rows = [(charlie_id, "charlie", "charlie@example.com", password_hash, now)]
user_ids.append(charlie_id)

for i in range(max(0, args.users - len(user_ids))):
    username = f"{fake.user_name()}{i}"
    created = fake.date_time_between(start_date="-1y", end_date="now", tzinfo=timezone.utc)
    new_id = str(uuid.uuid4())
    user_rows.append((new_id, username, f"{username}@example.com", password_hash, created))
    user_ids.append(new_id)

execute_values(cur, "INSERT INTO users (id, username, email, password_hash, created_at) VALUES %s", user_rows, page_size=2000)
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
                like_rows.append((str(uuid.uuid4()), post_id, liker, created + timedelta(seconds=random.randint(0, seconds_since_post))))

            n_comments = random.randint(0, args.max_comments)
            for _ in range(n_comments):
                comment_rows.append((str(uuid.uuid4()), post_id, random.choice(user_ids), random.choice(sentences),
                                     created + timedelta(seconds=random.randint(0, seconds_since_post))))

            image_url = f"/media/{uuid.uuid4()}." + random.choice(["jpg", "mp4"])
            content = " ".join(fake.words(nb=random.randint(1, 10)))

            post_rows.append((post_id, author_id, content, image_url, created, created, len(likers), n_comments))

    execute_values(cur, "INSERT INTO posts (id, author_id, content, image_url, created_at, updated_at, like_count, comment_count) VALUES %s",
                   post_rows, page_size=5000)
    execute_values(cur, "INSERT INTO likes (id, post_id, user_id, created_at) VALUES %s", like_rows, page_size=5000)
    execute_values(cur, "INSERT INTO comments (id, post_id, author_id, content, created_at) VALUES %s", comment_rows, page_size=5000)
    conn.commit()

    total_posts += len(post_rows)
    total_likes += len(like_rows)
    total_comments += len(comment_rows)
    print(f"  {min(start + args.batch, len(user_ids))}/{len(user_ids)} useri procesati", end="\r")

print()

cur.execute("CREATE INDEX IF NOT EXISTS idx_posts_author ON posts (author_id)")
cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_likes_post_user ON likes (post_id, user_id)")
cur.execute("CREATE INDEX IF NOT EXISTS idx_comments_post ON comments (post_id)")
conn.commit()

conn.autocommit = True
cur.execute("ANALYZE")
conn.autocommit = False

checks = {
    "postari cu autor inexistent": "SELECT COUNT(*) FROM posts p WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = p.author_id)",
    "like-uri fara postare": "SELECT COUNT(*) FROM likes l WHERE NOT EXISTS (SELECT 1 FROM posts p WHERE p.id = l.post_id)",
    "like-uri fara user": "SELECT COUNT(*) FROM likes l WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = l.user_id)",
    "comentarii fara postare": "SELECT COUNT(*) FROM comments c WHERE NOT EXISTS (SELECT 1 FROM posts p WHERE p.id = c.post_id)",
    "comentarii fara autor": "SELECT COUNT(*) FROM comments c WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = c.author_id)",
    "postari cu like_count gresit": "SELECT COUNT(*) FROM posts p LEFT JOIN (SELECT post_id, COUNT(*) n FROM likes GROUP BY post_id) x ON x.post_id = p.id WHERE p.like_count != COALESCE(x.n, 0)",
    "postari cu comment_count gresit": "SELECT COUNT(*) FROM posts p LEFT JOIN (SELECT post_id, COUNT(*) n FROM comments GROUP BY post_id) x ON x.post_id = p.id WHERE p.comment_count != COALESCE(x.n, 0)",
}
print("Verificare integritate (toate trebuie sa fie 0):")
for name, sql in checks.items():
    cur.execute(sql)
    print(f"  {name}: {cur.fetchone()[0]}")

cur.close()
conn.close()
print(f"Gata: {len(user_ids)} useri, {total_posts} postari, {total_likes} like-uri, "
      f"{total_comments} comentarii, in {time.time() - started:.1f} secunde.")