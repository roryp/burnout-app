"""Generate 7 friction point images using Azure OpenAI gpt-image-1.5 (Entra ID auth)"""
import json, base64, urllib.request, os, time
from azure.identity import DefaultAzureCredential

AZURE_OPENAI_ENDPOINT = "https://aoai-cengi3imeb5bg.openai.azure.com/"
DEPLOYMENT = "gpt-image-1.5"
API_VERSION = "2025-04-01-preview"
ENDPOINT = f"{AZURE_OPENAI_ENDPOINT}openai/deployments/{DEPLOYMENT}/images/generations?api-version={API_VERSION}"

# Authenticate via Entra ID
cred = DefaultAzureCredential()
token = cred.get_token("https://cognitiveservices.azure.com/.default").token
print(f"Authenticated via Entra ID")

OUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs", "images", "friction")
os.makedirs(OUT_DIR, exist_ok=True)

IMAGES = [
    {
        "file": "01-fragmentation.png",
        "prompt": (
            "A vivid editorial illustration showing a software developer at a desk drowning in FRAGMENTATION. "
            "The developer sits at the center with hands on head looking overwhelmed. Around them floats an explosion of "
            "overlapping browser tabs, IDE windows, Slack chat bubbles, email notifications with red badge counts, "
            "Jira kanban boards, GitHub pull request panels, terminal windows, video call pop-ups, and multiple monitors. "
            "Arrows criss-cross between all the windows showing constant context-switching. Each tool/window is a different "
            "color but the overall palette is cool blues and grays with red notification accents. "
            "Modern flat vector illustration style, clean white background. "
            "Small label 'FRAGMENTATION' centered at the bottom in a simple sans-serif font."
        )
    },
    {
        "file": "02-learning-pressure.png",
        "prompt": (
            "An editorial illustration showing LEARNING PRESSURE on a developer. "
            "A developer stands at the base of an enormous, impossibly tall staircase/mountain of technology books, "
            "framework logos, documentation pages, and online course certificates. Each step is labeled with tech names "
            "like 'AI/ML', 'LLMs', 'Kubernetes', 'Rust', 'New Framework'. The steps keep growing taller as they go up. "
            "Behind the developer, a treadmill conveyor belt pushes more books and tutorials toward them. "
            "A clock on the wall shows time racing forward. The developer looks small and daunted at the bottom. "
            "Color palette: warm oranges for the pressure/urgency, cool blues for the tech stack. "
            "Modern flat vector illustration, slightly editorial magazine style. Clean white background. "
            "Small label 'LEARNING PRESSURE' centered at the bottom in simple sans-serif font."
        )
    },
    {
        "file": "03-performance-standards.png",
        "prompt": (
            "An editorial illustration showing PERFORMANCE STANDARDS pressure on developers because AI exists. "
            "Split scene: left side shows a human developer working diligently at a desk with normal output (a few code files). "
            "Right side shows an AI robot producing a massive wall of code, PRs, and features at superhuman speed, "
            "with a giant speedometer/gauge showing '10x'. Between them, a manager figure holds a comparison chart "
            "pointing from the AI output to the human with raised expectations arrows going up. "
            "The human developer has a stress meter above their head going into the red zone. "
            "A conveyor belt of deadlines moves faster and faster. "
            "Color palette: cool blues and teals with red/orange stress indicators. "
            "Modern flat vector illustration, editorial style. Clean white background. "
            "Small label 'PERFORMANCE STANDARDS' centered at the bottom in simple sans-serif font."
        )
    },
    {
        "file": "04-isolation.png",
        "prompt": (
            "An editorial illustration showing developer ISOLATION. "
            "A single developer sits alone in a dark bubble/sphere, hunched over a laptop doing complex cognitive work "
            "(code architecture diagrams, security reviews, system design on screen). Outside the bubble, "
            "ghostly silhouettes of teammates are walking away or are shown as tiny video-call thumbnails that are greyed out. "
            "Empty chairs around a conference table visible in the background. The developer's face is lit only by the laptop glow. "
            "Chat messages with no replies float nearby. A disconnected network cable hangs from the desk. "
            "Inside the bubble: cold blue tones. Outside: warm collaborative yellows/greens (showing what's missing). "
            "Modern flat vector illustration, slightly melancholic editorial style. Clean white background. "
            "Small label 'ISOLATION' centered at the bottom in simple sans-serif font."
        )
    },
    {
        "file": "05-interface-friction.png",
        "prompt": (
            "An editorial illustration showing INTERFACE FRICTION — fighting clunky developer tools. "
            "A developer is shown wrestling/pushing against a giant tangled machine made of gears, levers, error dialogs, "
            "loading spinners, progress bars stuck at 99%, broken build notifications, cryptic stack traces, "
            "and convoluted settings panels. The developer is physically straining to push through. "
            "Sticky notes with 'TODO: fix tooling' are everywhere. A simple task (shown as a small glowing orb labeled 'Ship Feature') "
            "is visible on the far side of the machine but blocked by the mess. "
            "A winding, tangled path with hurdles represents the workflow vs. a clean straight arrow showing the ideal path. "
            "Color palette: grays and muted tones for the clunky tools, bright teal/green for the goal. "
            "Modern flat vector illustration, editorial style. Clean white background. "
            "Small label 'INTERFACE FRICTION' centered at the bottom in simple sans-serif font."
        )
    },
    {
        "file": "06-altitude-sickness.png",
        "prompt": (
            "An editorial illustration showing developer ALTITUDE SICKNESS — being pulled between strategic high-level "
            "thinking and fine-grained execution simultaneously. "
            "A developer is literally stretched between two planes: their upper body is in the clouds at a whiteboard "
            "doing architecture diagrams, system design, and strategic roadmap planning (shown with cloud-level icons: "
            "architecture blueprints, roadmap timelines, big-picture diagrams). "
            "Their lower body is pulled down to ground level doing code reviews, bug fixes, pull request comments, "
            "and line-by-line debugging (shown with magnifying glass over code, red bug icons, PR diffs). "
            "The developer is being physically stretched/torn between these two altitudes with elastic bands. "
            "Upper zone: light sky blue. Lower zone: dark detailed ground. "
            "Modern flat vector illustration, editorial style. Clean white background. "
            "Small label 'ALTITUDE SICKNESS' centered at the bottom in simple sans-serif font."
        )
    },
    {
        "file": "07-workload-creep.png",
        "prompt": (
            "An editorial illustration showing WORKLOAD CREEP — AI makes more possible so more gets expected. "
            "A developer stands next to a funnel/hopper. At the top, an AI robot pours in a massive flood of generated code, "
            "features, PRs, documentation, and tasks. The funnel grows wider and wider at the top. "
            "At the bottom, the developer must review, test, deploy, and maintain everything coming out — "
            "shown as an ever-growing pile of work tickets stacking up around them. A 'capacity' meter on the developer "
            "is overflowing. Expectation arrows keep pointing UP while the developer's energy bar is depleting DOWN. "
            "A boss character adds more items saying 'AI can handle it'. The pile of work grows like a rising tide around the developer's ankles, then knees, then waist. "
            "Color palette: cool blues and teals with orange/red overload indicators. "
            "Modern flat vector illustration, editorial style. Clean white background. "
            "Small label 'WORKLOAD CREEP' centered at the bottom in simple sans-serif font."
        )
    },
]

for i, img in enumerate(IMAGES):
    print(f"[{i+1}/7] Generating {img['file']}...", flush=True)
    payload = json.dumps({
        "prompt": img["prompt"],
        "n": 1,
        "size": "1536x1024",
        "quality": "high",
        "output_format": "png"
    }).encode("utf-8")

    req = urllib.request.Request(
        ENDPOINT,
        data=payload,
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {token}"},
        method="POST"
    )

    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            data = json.loads(resp.read().decode("utf-8"))

        img_bytes = base64.b64decode(data["data"][0]["b64_json"])
        out_path = os.path.join(OUT_DIR, img["file"])
        with open(out_path, "wb") as f:
            f.write(img_bytes)
        print(f"  Saved {img['file']} ({len(img_bytes):,} bytes)", flush=True)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        print(f"  ERROR {e.code}: {body[:500]}", flush=True)
    except Exception as e:
        print(f"  ERROR generating {img['file']}: {e}", flush=True)

    if i < len(IMAGES) - 1:
        time.sleep(2)  # small delay between calls

print("\nDone! All images saved to docs/images/friction/")
