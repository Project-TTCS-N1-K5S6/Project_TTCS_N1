<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${issueCode} | Task Detail</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        :root {
            --bg: #f4f5f7;
            --panel: rgba(255,255,255,0.32);
            --panel-strong: rgba(255,255,255,0.72);
            --border: #dfe3e8;
            --text: #1d232c;
            --muted: #667382;
            --soft: #f7f8fb;
            --accent: #b487e5;
            --blue: #4a7af7;
            --gold: #f4d77a;
            --shadow: rgba(18,24,38,0.04);
        }

        * { box-sizing: border-box; }
        html, body {
            margin: 0;
            font-family: 'Inter', sans-serif;
            background: var(--bg);
            color: var(--text);
            height: 100%;
        }
        body { min-height: 100vh; }

        .page {
            height: 100vh;
            display: flex;
            flex-direction: column;
            background: var(--bg);
        }

        .topbar {
            height: 60px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 16px 0 12px;
            background: rgba(255,255,255,0.28);
            border-bottom: 1px solid var(--border);
        }

        .topbar-left {
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 13px;
            color: #58657a;
        }

        .issue-pill {
            display: inline-flex;
            align-items: center;
            gap: 7px;
            font-weight: 600;
            color: #5a6678;
        }

        .issue-pill .dot {
            width: 10px;
            height: 10px;
            border-radius: 3px;
            background: linear-gradient(135deg, #7ea3ff, #b783e8);
            border: 1px solid rgba(0,0,0,0.06);
            display: inline-block;
        }

        .issue-pill.muted {
            color: #7a8597;
            opacity: 0.9;
        }

        .topbar-right {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .icon-btn {
            width: 30px;
            height: 30px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border-radius: 6px;
            border: 1px solid var(--border);
            background: rgba(255,255,255,0.2);
            color: #677684;
            font-size: 13px;
        }

        .badge-btn {
            position: relative;
        }

        .badge-btn::after {
            content: '1';
            position: absolute;
            top: -4px;
            right: -2px;
            min-width: 13px;
            height: 13px;
            border-radius: 50%;
            background: #dfe2e7;
            color: #1d2430;
            font-size: 9px;
            line-height: 13px;
            text-align: center;
            font-weight: 700;
            border: 1px solid #cfd6de;
        }

        .workspace {
            flex: 1;
            display: flex;
            min-height: 0;
        }

        .main-panel {
            flex: 1.7;
            padding: 16px 18px 18px 18px;
            min-width: 0;
        }

        .scroll-region {
            height: calc(100vh - 60px);
            overflow-y: auto;
            padding-right: 8px;
        }

        .title-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
        }

        .page-title {
            margin: 10px 0 18px;
            font-size: clamp(28px, 2vw, 42px);
            line-height: 1.1;
            letter-spacing: -0.04em;
            font-weight: 700;
            color: #1f2430;
        }

        .title-actions {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 8px;
        }

        .mini-btn {
            width: 30px;
            height: 30px;
            border: 1px solid var(--border);
            background: rgba(255,255,255,0.3);
            border-radius: 8px;
            color: #4d5869;
            display: inline-flex;
            align-items: center;
            justify-content: center;
        }

        .section {
            border: 1px solid #e0e4ea;
            border-radius: 10px;
            background: rgba(255,255,255,0.22);
            margin-bottom: 18px;
            overflow: hidden;
        }

        .section-header {
            padding: 12px 14px 11px;
            border-bottom: 1px solid #e0e4ea;
            background: rgba(255,255,255,0.08);
            font-size: 14px;
            font-weight: 600;
            color: #1a2129;
        }

        .section-header .label {
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        .caret {
            display: inline-block;
            width: 9px;
            height: 9px;
            border-left: 2px solid #6d798a;
            border-bottom: 2px solid #6d798a;
            transform: rotate(-45deg);
        }

        .description-box {
            padding: 18px 14px 16px;
        }

        .timeline {
            list-style: none;
            margin: 0;
            padding: 0;
            position: relative;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .timeline::before {
            content: "";
            position: absolute;
            left: 7px;
            top: 5px;
            bottom: 5px;
            width: 2px;
            background: #dfe5ee;
        }

        .timeline-item {
            padding-left: 24px;
            position: relative;
            font-size: 15px;
            color: #2d3643;
            line-height: 1.5;
        }

        .timeline-item::before {
            content: "";
            position: absolute;
            left: 0;
            top: 5px;
            width: 10px;
            height: 10px;
            background: #8e9bb2;
            border-radius: 50%;
            border: 2px solid #fff;
            box-shadow: 0 0 0 1px #dfe5ee;
        }

        .timeline-item .title {
            font-weight: 600;
            color: #1d2431;
        }

        .timeline-item .meta {
            margin-left: 6px;
            font-size: 12px;
            color: #67758b;
        }

        .subtasks, .linked-items, .confluence, .agents, .activity {
            margin-top: 12px;
        }

        .heading-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;
            margin: 12px 2px 10px;
            font-size: 17px;
            font-weight: 600;
            color: #1d2430;
        }

        .heading-row .label {
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        .heading-row .chevron {
            font-size: 15px;
            color: #687681;
        }

        .simple-link {
            margin-top: 4px;
            color: #6f7d8f;
            font-size: 14px;
        }

        .confluence-box {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 14px;
            border: 1px solid var(--border);
            border-radius: 10px;
            background: rgba(255,255,255,0.2);
            min-height: 52px;
            padding: 12px 14px;
        }

        .confluence-left {
            display: flex;
            align-items: center;
            gap: 10px;
            color: #1d2430;
            font-weight: 500;
        }

        .small-icon {
            width: 20px;
            height: 20px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border-radius: 6px;
            background: rgba(163,172,184,0.15);
            color: #7e8aa0;
        }

        .start-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 96px;
            height: 32px;
            padding: 0 12px;
            border-radius: 8px;
            background: linear-gradient(180deg, #e9d7f5, #d9bef3);
            border: 1px solid rgba(120,87,154,0.15);
            color: #513d67;
            font-size: 13px;
            font-weight: 600;
        }

        .agent-box {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            min-height: 52px;
            padding: 10px 14px;
            background: rgba(255,255,255,0.18);
            border: 1px solid var(--border);
            border-radius: 10px;
        }

        .agent-left {
            display: flex;
            align-items: center;
            gap: 10px;
            font-weight: 500;
            color: #1f2531;
        }

        .agent-dot {
            width: 18px;
            height: 18px;
            border-radius: 50%;
            background: radial-gradient(circle at 35% 35%, #70e09f, #2ca766 60%, #1f7a4b);
            border: 1px solid rgba(34,94,52,0.12);
            box-shadow: inset 0 0 0 2px rgba(255,255,255,0.75);
        }

        .activity-card {
            border: 1px solid var(--border);
            border-radius: 12px;
            overflow: hidden;
            background: rgba(255,255,255,0.22);
        }

        .tabs {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 10px 12px 0;
        }

        .tab {
            padding: 8px 12px 10px;
            border-radius: 8px 8px 0 0;
            border: 1px solid transparent;
            border-bottom: none;
            background: transparent;
            color: #59677b;
            font-size: 13px;
            font-weight: 500;
        }

        .tab.active {
            background: #fff;
            border-color: var(--border);
            color: #1c2938;
            box-shadow: 0 -1px 0 rgba(0,0,0,0.01);
        }

        .comment-box {
            display: flex;
            align-items: center;
            gap: 12px;
            border-top: 1px solid var(--border);
            padding: 12px;
            background: rgba(255,255,255,0.08);
        }

        .avatar {
            width: 28px;
            height: 28px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            background: linear-gradient(135deg, #3ac7db, #5d58d9);
            color: #fff;
            font-weight: 700;
            font-size: 11px;
        }

        .comment-input {
            flex: 1;
            min-height: 46px;
            border-radius: 10px;
            border: 1px solid var(--border);
            background: rgba(255,255,255,0.75);
            color: #334052;
            padding: 11px 14px;
            font-size: 15px;
        }

        .comment-input::placeholder {
            color: #73819c;
        }

        .quick-actions {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            padding: 0 12px 12px;
            background: rgba(255,255,255,0.06);
        }

        .chip {
            padding: 5px 10px;
            border: 1px solid var(--border);
            background: #f4f6fa;
            color: #59687d;
            border-radius: 8px;
            font-size: 12px;
        }

        .side-panel {
            width: 420px;
            min-width: 320px;
            padding: 16px 18px 18px 10px;
            border-left: 1px solid rgba(189,196,208,0.7);
            background: rgba(255,255,255,0.14);
        }

        .sidebar-scroll {
            height: calc(100vh - 60px);
            overflow-y: auto;
            padding-right: 8px;
        }

        .side-card {
            background: rgba(255,255,255,0.74);
            border: 1px solid #dfe4ea;
            border-radius: 12px;
            overflow: hidden;
            margin-bottom: 14px;
        }

        .side-card .section-header {
            padding: 12px 14px;
            background: rgba(255,255,255,0.08);
            border-bottom: 1px solid #dfe4ea;
            font-size: 14px;
        }

        .details-content {
            padding: 10px 14px 12px;
        }

        .row {
            display: flex;
            align-items: center;
            gap: 12px;
            min-height: 36px;
            padding: 2px 0;
        }

        .row-label {
            width: 104px;
            flex-shrink: 0;
            color: #677487;
            font-size: 14px;
            font-weight: 500;
            line-height: 1.4;
        }

        .row-value {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: flex-start;
            min-height: 34px;
            color: #1c2430;
            font-size: 14px;
            gap: 8px;
        }

        .chip-user {
            padding: 3px 8px 3px 4px;
            border-radius: 999px;
            background: #f4f5f7;
            border: 1px solid rgba(152,162,174,0.35);
            display: inline-flex;
            align-items: center;
            gap: 7px;
        }

        .user-badge {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            display: grid;
            place-items: center;
            background: linear-gradient(135deg, #4d78ff, #6e58df);
            color: #fff;
            font-size: 10px;
            font-weight: 700;
        }

        .parent-link {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            color: #1b232f;
            padding: 4px 8px;
            border-radius: 6px;
            background: rgba(246,205,65,0.12);
            border: 1px solid rgba(221,171,38,0.2);
        }

        .parent-dot {
            width: 10px;
            height: 10px;
            border-radius: 3px;
            background: linear-gradient(135deg, #ffd96a, #f0b930);
            box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05);
        }

        .story-pill {
            min-width: 24px;
            height: 22px;
            border-radius: 4px;
            border: 1px solid #dfe4ea;
            background: #edf0f5;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            font-weight: 700;
            color: #2d3746;
            padding: 0 6px;
        }

        .reporter {
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        .reporter .user-badge {
            width: 24px;
            height: 24px;
            background: linear-gradient(135deg, #0db7c7, #17b9a5);
            font-size: 10px;
        }

        .toggle-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            padding: 12px 14px;
            font-size: 15px;
            font-weight: 600;
            color: #253040;
        }

        .toggle-row .chevron-right { font-size: 16px; color: #687581; }

        .mini-meta {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            color: #677487;
            padding: 0 14px 12px;
        }

        .mini-meta .badge {
            padding: 3px 8px;
            border-radius: 700px;
            background: #f0f4f8;
            border: 1px solid #dae1ea;
            color: #4f5b6a;
            font-weight: 600;
        }

        @media (max-width: 1100px){
            .workspace { flex-direction: column; }
            .side-panel { width: 100%; border-left: none; border-top: 1px solid rgba(189,196,208,0.7); }
        }
    </style>
</head>
<body>
    <div class="page">
        <header class="topbar">
            <div class="topbar-left">
                <span class="issue-pill"><span class="dot"></span> ${parentCode}</span>
                <span class="issue-pill muted">/ ${issueCode}</span>
            </div>
            <div class="topbar-right">
                <button class="icon-btn"><i class="bi bi-lock"></i></button>
                <button class="icon-btn badge-btn"><i class="bi bi-bell"></i></button>
                <button class="icon-btn"><i class="bi bi-arrow-clockwise"></i></button>
                <button class="icon-btn"><i class="bi bi-gear"></i></button>
                <button class="icon-btn"><i class="bi bi-x-lg"></i></button>
            </div>
        </header>

        <div class="workspace">
            <main class="main-panel">
                <div class="scroll-region">
                    <div class="title-row">
                        <h1 class="page-title">Nhật ký tương tác ứng viên</h1>
                        <div class="title-actions">
                            <button class="mini-btn"><i class="bi bi-plus-lg"></i></button>
                            <button class="mini-btn"><i class="bi bi-dash-lg"></i></button>
                        </div>
                    </div>

                    <section class="section">
                        <div class="section-header">
                            <span class="label"><span class="caret"></span>Description</span>
                        </div>
                        <div class="description-box">
                            <ul class="timeline">
                                <c:forEach var="event" items="${timeline}">
                                    <li class="timeline-item">
                                        <span>
                                            <span class="title">${event.title}</span>
                                            <c:if test="${event.meta != null && !empty event.meta}">
                                                <span class="meta">${event.meta}</span>
                                            </c:if>
                                        </span>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </section>

                    <div class="subtasks">
                        <div class="heading-row"><span class="label"><span class="chevron">›</span> Subtasks</span></div>
                    </div>

                    <div class="linked-items">
                        <div class="heading-row"><span class="label">Linked work items</span></div>
                        <div class="simple-link">Add linked work item</div>
                    </div>

                    <div class="confluence">
                        <div class="heading-row"><span class="label">Confluence content</span></div>
                        <div class="confluence-box">
                            <div class="confluence-left">
                                <span class="small-icon"><i class="bi bi-file-earmark-text"></i></span>
                                <span>…</span>
                            </div>
                            <button class="start-btn">Get started</button>
                        </div>
                    </div>

                    <div class="agents">
                        <div class="heading-row" style="margin-top: 18px; margin-bottom: 12px;">
                            <span class="label">Agents</span>
                        </div>
                        <div class="agent-box">
                            <div class="agent-left"><span class="agent-dot"></span> <span>Start work</span></div>
                            <div class="agent-right"><i class="bi bi-caret-down-fill"></i></div>
                        </div>
                    </div>

                    <div class="activity" style="margin-top: 20px;">
                        <div class="heading-row" style="margin-top: 2px; margin-bottom: 8px;"><span class="label">Activity</span></div>
                        <div class="activity-card">
                            <div class="tabs">
                                <button class="tab active">All</button>
                                <button class="tab">Comments</button>
                                <button class="tab">History</button>
                                <button class="tab">Work log</button>
                            </div>
                            <div class="comment-box">
                                <div class="avatar">${currentUserInitials}</div>
                                <input class="comment-input" type="text" placeholder="Add a comment..." aria-label="Add a comment" />
                            </div>
                            <div class="quick-actions">
                                <span class="chip">Status update...</span>
                                <span class="chip">Thanks...</span>
                                <span class="chip">Agree...</span>
                            </div>
                        </div>
                    </div>
                </div>
            </main>

            <aside class="side-panel">
                <div class="sidebar-scroll">
                    <div class="side-card">
                        <div class="section-header">
                            <span class="label"><span class="caret"></span>Details</span>
                        </div>
                        <div class="details-content">
                            <div class="row">
                                <div class="row-label">Assignee</div>
                                <div class="row-value">
                                    <span class="chip-user">
                                        <span class="user-badge">${currentUserInitials}</span>
                                        <span>${assignee}</span>
                                    </span>
                                </div>
                            </div>
                            <div class="row">
                                <div class="row-label">Parent</div>
                                <div class="row-value">
                                    <span class="parent-link">
                                        <span class="parent-dot"></span>
                                        ${parentCode} Hồ sơ ứng viên &amp; ...
                                    </span>
                                </div>
                            </div>
                            <div class="row">
                                <div class="row-label">Due date</div>
                                <div class="row-value"><span>Add due date</span></div>
                            </div>
                            <div class="row">
                                <div class="row-label">Labels</div>
                                <div class="row-value"><span>Add labels</span></div>
                            </div>
                            <div class="row">
                                <div class="row-label">Team</div>
                                <div class="row-value"><span>Add team</span></div>
                            </div>
                            <div class="row">
                                <div class="row-label">Start date</div>
                                <div class="row-value"><span>Add date</span></div>
                            </div>
                            <div class="row">
                                <div class="row-label">Sprint</div>
                                <div class="row-value"><a href="#" style="color:#4b7df2; text-decoration:none; font-weight:500;">${sprint}</a></div>
                            </div>
                            <div class="row">
                                <div class="row-label">Story point estimate</div>
                                <div class="row-value"><span class="story-pill">${storyPoint}</span></div>
                            </div>
                            <div class="row">
                                <div class="row-label">Reporter</div>
                                <div class="row-value">
                                    <span class="reporter">
                                        <span class="user-badge">${reporterInitials}</span>
                                        <span>${reporter}</span>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="side-card">
                        <div class="toggle-row">
                            <span>Development</span>
                            <span class="chevron-right">›</span>
                        </div>
                    </div>

                    <div class="side-card">
                        <div class="toggle-row">
                            <span>Automation</span>
                            <span class="chevron-right">›</span>
                        </div>
                        <div class="mini-meta">
                            <span class="badge">Rule executions</span>
                        </div>
                        <div class="details-content" style="padding-top:0;">
                            <div class="row">
                                <div class="row-label" style="width:120px;">Created</div>
                                <div class="row-value"><span>1 hour ago</span></div>
                            </div>
                            <div class="row">
                                <div class="row-label" style="width:120px;">Updated</div>
                                <div class="row-value"><span>1 minute ago</span></div>
                            </div>
                        </div>
                    </div>
                </div>
            </aside>
        </div>
    </div>
</body>
</html>
