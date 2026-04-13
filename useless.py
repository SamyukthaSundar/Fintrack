import mysql.connector

conn = mysql.connector.connect(
    host="mainline.proxy.rlwy.net",
    user="root",
    password="oasiKVgHXqILMGFzmLTQseinRQLiaDhI",
    database="railway",
    port=32510
)

cursor = conn.cursor()

with open("schema.sql", "r") as file:
    sql_commands = file.read()

for command in sql_commands.split(";"):
    if command.strip():
        cursor.execute(command)

conn.commit()
print("Schema created!")