import random
from datetime import datetime, timedelta
from fastapi import FastAPI, Query

app = FastAPI()

# ── helpers ──────────────────────────────────────────────────────────────────

GREETINGS_EN = [
    "Hey, are you there?",
    "Don't forget about tonight!",
    "Can we reschedule?",
    "Just sent you the file.",
    "Call me when you're free.",
    "Did you see the news?",
    "Thanks for yesterday!",
    "I'll be a bit late.",
]

GREETINGS_RU = [
    "Привет, ты здесь?",
    "Не забудь про сегодняшний вечер!",
    "Можем перенести встречу?",
    "Только что отправил файл.",
    "Позвони, когда освободишься.",
    "Ты видел новости?",
    "Спасибо за вчера!",
    "Немного опоздаю.",
]

FIRST_NAMES = ["Alice", "Bob", "Carlos", "Diana", "Eve", "Frank", "Grace", "Hank"]
LAST_NAMES  = ["Smith", "Jones", "Garcia", "Brown", "Lee", "Wilson", "Taylor", "Davis"]

REACTION_POOL = ["👍", "❤️", "😂", "😮", "😢", "🔥"]

CHAT_NAMES = ["Team Alpha", "Family", "Book Club", "Work Crew", "Old Friends", "Game Night"]


def random_sender() -> dict:
    return {
        "id": random.randint(1000, 9999),
        "name": f"{random.choice(FIRST_NAMES)} {random.choice(LAST_NAMES)}",
        "avatar_url": f"https://i.pravatar.cc/150?u={random.randint(1, 500)}",
    }


def random_ts(days_back: int = 7) -> str:
    delta = timedelta(
        days=random.randint(0, days_back),
        hours=random.randint(0, 23),
        minutes=random.randint(0, 59),
    )
    return (datetime.utcnow() - delta).strftime("%Y-%m-%dT%H:%M:%SZ")


# ── endpoints ─────────────────────────────────────────────────────────────────

@app.get("/last_message")
def last_message(
    lang: str = Query(default="en", pattern="^(en|ru)$", description="Response language: en or ru"),
    include_reactions: bool = Query(default=False, description="Include emoji reactions"),
):
    """Return a randomly generated last chat message."""
    pool = GREETINGS_RU if lang == "ru" else GREETINGS_EN
    response = {
        "id": random.randint(100_000, 999_999),
        "text": random.choice(pool),
        "sent_at": random_ts(days_back=3),
        "read": random.choice([True, False]),
        "sender": random_sender(),
    }
    if include_reactions:
        response["reactions"] = [
            {"emoji": random.choice(REACTION_POOL), "count": random.randint(1, 12)}
            for _ in range(random.randint(1, 3))
        ]
    return response


@app.get("/chats")
def chats(
    limit: int = Query(default=5, ge=1, le=20, description="How many chats to return"),
    unread_only: bool = Query(default=False, description="Return only chats with unread messages"),
):
    """Return a list of randomly generated chat previews."""
    items = []
    for _ in range(limit):
        unread_count = random.randint(0, 15)
        if unread_only and unread_count == 0:
            unread_count = random.randint(1, 15)
        items.append({
            "chat_id": random.randint(1, 9999),
            "name": random.choice(CHAT_NAMES),
            "unread_count": unread_count,
            "is_muted": random.choice([True, False]),
            "last_activity": {
                "text": random.choice(GREETINGS_EN),
                "sent_at": random_ts(days_back=7),
                "author": random.choice(FIRST_NAMES),
            },
        })
    return {"total": len(items), "chats": items}
