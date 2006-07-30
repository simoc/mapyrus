<script type="text/ecmascript">
<![CDATA[
function highlight(evt, color)
{
alert("event at " + evt.clientX + ", " + evt.clientY);
evt.target.setAttribute("style", "fill:" + color);
}
]]></script>
